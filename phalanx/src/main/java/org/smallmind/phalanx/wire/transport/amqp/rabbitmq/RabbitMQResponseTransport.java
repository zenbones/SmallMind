/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.phalanx.worker.WorkManager;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.WorkerFactory;

/**
 * RabbitMQ-backed response transport that consumes inbound requests, dispatches them to invocation
 * workers, and publishes results back to the calling instance.
 *
 * <p>All of its routers share a single connection, owned by a {@link RabbitMQConnectionManager}. That
 * is a requirement rather than an economy: every router declares the same per-instance queue names, and
 * an exclusive queue belongs to a connection, so a connection per router would mean a queue per router
 * and a shout delivered once per router instead of once.
 */
public class RabbitMQResponseTransport extends WorkManager<InvocationWorker, RabbitMQMessage> implements WorkerFactory<InvocationWorker, RabbitMQMessage>, ResponseTransport, ResponseTransmitter {

  //  Three, and not negotiable downward. A quorum queue needs a majority of its members, so two
  //  replicas tolerate no node failures at all and are worse than the classic queue they replace.
  private static final int DEFAULT_QUORUM_REPLICATION_COUNT = 3;

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final RabbitMQConnectionManager connectionManager;
  private final SignalCodec signalCodec;
  private final ConcurrentLinkedQueue<ResponseMessageRouter> responseQueue;
  private final ResponseMessageRouter[] responseMessageRouters;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  /**
   * Creates a response transport with the default talk queue replication count of three.
   *
   * @param rabbitMQConnector            source of connections and retry tuning.
   * @param nameConfiguration            exchange and queue naming scheme.
   * @param workerClass                  worker class used for invocation handling.
   * @param signalCodec                  codec for serializing and deserializing signals.
   * @param serviceGroup                 service group name embedded in AMQP routing keys.
   * @param clusterSize                  number of response routers to create.
   * @param concurrencyLimit             maximum number of concurrent invocation workers.
   * @param messageTTLSeconds            message time-to-live in seconds.
   * @param autoAcknowledge              whether consumers should auto-ack delivered messages.
   * @param publisherConfirmationHandler optional handler for publisher confirms; may be {@code null}.
   * @throws IOException          if the transport cannot be started.
   * @throws InterruptedException if startup is interrupted.
   * @throws TimeoutException     if startup times out.
   */
  public RabbitMQResponseTransport (RabbitMQConnector rabbitMQConnector, NameConfiguration nameConfiguration, Class<InvocationWorker> workerClass, SignalCodec signalCodec, String serviceGroup, int clusterSize, int concurrencyLimit, int messageTTLSeconds, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler)
    throws IOException, InterruptedException, TimeoutException {

    this(rabbitMQConnector, nameConfiguration, workerClass, signalCodec, serviceGroup, clusterSize, concurrencyLimit, messageTTLSeconds, DEFAULT_QUORUM_REPLICATION_COUNT, autoAcknowledge, publisherConfirmationHandler);
  }

  /**
   * Creates a response transport with an explicit talk queue replication count.
   *
   * @param rabbitMQConnector            source of connections and retry tuning.
   * @param nameConfiguration            exchange and queue naming scheme.
   * @param workerClass                  worker class used for invocation handling.
   * @param signalCodec                  codec for serializing and deserializing signals.
   * @param serviceGroup                 service group name embedded in AMQP routing keys.
   * @param clusterSize                  number of response routers to create.
   * @param concurrencyLimit             maximum number of concurrent invocation workers.
   * @param messageTTLSeconds            message time-to-live in seconds.
   * @param quorumReplicationCount       replica count for the shared talk queue; do not set this below
   *                                     three, since a quorum queue needs a majority of its members and
   *                                     two replicas tolerate no node failure at all.
   * @param autoAcknowledge              whether consumers should auto-ack delivered messages.
   * @param publisherConfirmationHandler optional handler for publisher confirms; may be {@code null}.
   * @throws IOException          if the transport cannot be started.
   * @throws InterruptedException if startup is interrupted.
   * @throws TimeoutException     if startup times out.
   */
  public RabbitMQResponseTransport (RabbitMQConnector rabbitMQConnector, NameConfiguration nameConfiguration, Class<InvocationWorker> workerClass, SignalCodec signalCodec, String serviceGroup, int clusterSize, int concurrencyLimit, int messageTTLSeconds, int quorumReplicationCount, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler)
    throws IOException, InterruptedException, TimeoutException {

    super(workerClass, concurrencyLimit);

    int routerIndex = 0;

    this.signalCodec = signalCodec;

    //  One connection for the whole transport, shared by every router. The per-instance queues are
    //  declared exclusively, and an exclusive queue belongs to a connection - a router per connection
    //  would mean a queue per router, and a shout or whisper delivered once per router instead of once.
    connectionManager = new RabbitMQConnectionManager(rabbitMQConnector, "response[" + serviceGroup + "]");

    responseMessageRouters = new ResponseMessageRouter[clusterSize];
    for (int index = 0; index < responseMessageRouters.length; index++) {
      responseMessageRouters[index] = new ResponseMessageRouter(connectionManager, nameConfiguration, this, signalCodec, serviceGroup, instanceId, index, messageTTLSeconds, quorumReplicationCount, autoAcknowledge, publisherConfirmationHandler);
    }

    connectionManager.start();

    responseQueue = new ConcurrentLinkedQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      responseQueue.add(responseMessageRouters[routerIndex]);
      if (++routerIndex == responseMessageRouters.length) {
        routerIndex = 0;
      }
    }

    startUp(this);
  }

  /**
   * Returns the manager owning this transport's connection, so that a test can drop the connection the
   * way a broker would. Package private on purpose - nothing outside this package should be reaching
   * past the transport to its connection.
   *
   * @return this transport's connection manager.
   */
  RabbitMQConnectionManager getConnectionManager () {

    return connectionManager;
  }

  /**
   * Returns the unique instance id used by callers to whisper at this node specifically.
   *
   * @return this responder's instance id.
   */
  @Override
  public String getInstanceId () {

    return instanceId;
  }

  /**
   * Registers a service implementation with the invocation circuit.
   *
   * @param serviceInterface interface identifying which inbound requests route here.
   * @param targetService    the wired service implementation.
   * @return this responder's instance id, for whisper routing.
   * @throws Exception if registration fails.
   */
  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  /**
   * Creates a worker to handle inbound invocation messages.
   *
   * @param transferQueue queue supplying messages to the worker.
   * @return a new invocation worker.
   */
  @Override
  public InvocationWorker createWorker (WorkQueue<RabbitMQMessage> transferQueue) {

    return new InvocationWorker(transferQueue, this, invocationCircuit, signalCodec);
  }

  /**
   * Returns the lifecycle state this transport was last told to be in, which is not the same question
   * as whether it is working - see {@link #isHealthy()}.
   *
   * @return current transport state.
   */
  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  /**
   * Returns whether this transport is genuinely connected, bound and consuming - which the lifecycle
   * state alone could never tell anyone, since it reports PLAYING whether or not a single consumer is
   * attached to a single queue. A transport that has been deliberately paused is healthy but idle.
   *
   * @return true if the transport is able to carry traffic.
   */
  @Override
  public boolean isHealthy () {

    return (!closed.get()) && connectionManager.isHealthy();
  }

  /**
   * Returns a description of the transport's connection, binding and consumer state.
   *
   * @return diagnostic description intended for an operator.
   */
  @Override
  public String getDiagnostic () {

    return connectionManager.getDiagnostic();
  }

  /**
   * Resumes consumption across every router.
   *
   * @throws Exception if a router cannot resume.
   */
  @Override
  public void play ()
    throws Exception {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PAUSED, TransportState.PLAYING)) {
        for (ResponseMessageRouter responseMessageRouter : responseMessageRouters) {
          responseMessageRouter.play();
        }
      }
    }
  }

  /**
   * Suspends consumption across every router, leaving the queues in place so that messages arriving
   * meanwhile are held rather than lost. The transport stays paused across a reconnect.
   *
   * @throws Exception if a router cannot be paused.
   */
  @Override
  public void pause ()
    throws Exception {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PLAYING, TransportState.PAUSED)) {
        for (ResponseMessageRouter responseMessageRouter : responseMessageRouters) {
          responseMessageRouter.pause();
        }
      }
    }
  }

  /**
   * Publishes a result to the calling instance, using the next router from the pool.
   *
   * @param callerId      identifier of the caller to reply to.
   * @param correlationId correlation id matching the original request.
   * @param error         whether the result represents an error.
   * @param nativeType    native type information for the result.
   * @param result        payload to send.
   * @throws Throwable if no router is available, or publishing fails.
   */
  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable {

    ResponseMessageRouter responseMessageRouter;

    if ((responseMessageRouter = responseQueue.poll()) == null) {
      throw new TransportException("Unable to take a ResponseMessageRouter, which should never happen - please contact your system administrator");
    }

    responseMessageRouter.publish(callerId, correlationId, error, nativeType, result);
    responseQueue.add(responseMessageRouter);
  }

  /**
   * Closes the transport, shutting down the shared connection and the worker pool.
   *
   * @throws IOException          if closing the connection fails.
   * @throws InterruptedException if shutdown is interrupted.
   * @throws TimeoutException     if shutdown times out.
   */
  @Override
  public void close ()
    throws IOException, InterruptedException, TimeoutException {

    if (closed.compareAndSet(false, true)) {
      synchronized (transportStateRef) {
        transportStateRef.set(TransportState.CLOSED);

        connectionManager.close();

        shutDown();
      }
    }
  }
}
