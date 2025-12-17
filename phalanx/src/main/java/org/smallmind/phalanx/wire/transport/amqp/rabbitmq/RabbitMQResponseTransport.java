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
 * RabbitMQ-based response transport that consumes requests and publishes results.
 */
public class RabbitMQResponseTransport extends WorkManager<InvocationWorker, RabbitMQMessage> implements WorkerFactory<InvocationWorker, RabbitMQMessage>, ResponseTransport, ResponseTransmitter {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final SignalCodec signalCodec;
  private final ConcurrentLinkedQueue<ResponseMessageRouter> responseQueue;
  private final ResponseMessageRouter[] responseMessageRouters;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  /**
   * @param rabbitMQConnector            connector for creating channels.
   * @param enduringQueueContractor      contractor for durable queues (talk).
   * @param ephemeralQueueContractor     contractor for ephemeral queues (shout/whisper).
   * @param nameConfiguration            exchange/queue naming scheme.
   * @param workerClass                  worker implementation for invocation handling.
   * @param signalCodec                  codec for serialization.
   * @param serviceGroup                 service group name used in routing keys.
   * @param clusterSize                  number of routers to create.
   * @param concurrencyLimit             worker concurrency limit.
   * @param messageTTLSeconds            message time-to-live in seconds.
   * @param autoAcknowledge              whether consumers should auto-ack.
   * @param publisherConfirmationHandler optional handler for publisher confirms, may be null.
   * @throws IOException          if router initialization fails.
   * @throws InterruptedException if initialization is interrupted.
   * @throws TimeoutException     if initialization times out.
   */
  public RabbitMQResponseTransport (RabbitMQConnector rabbitMQConnector, QueueContractor enduringQueueContractor, QueueContractor ephemeralQueueContractor, NameConfiguration nameConfiguration, Class<InvocationWorker> workerClass, SignalCodec signalCodec, String serviceGroup, int clusterSize, int concurrencyLimit, int messageTTLSeconds, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler)
    throws IOException, InterruptedException, TimeoutException {

    super(workerClass, concurrencyLimit);

    int routerIndex = 0;

    this.signalCodec = signalCodec;

    responseMessageRouters = new ResponseMessageRouter[clusterSize];
    for (int index = 0; index < responseMessageRouters.length; index++) {
      responseMessageRouters[index] = new ResponseMessageRouter(rabbitMQConnector, enduringQueueContractor, ephemeralQueueContractor, nameConfiguration, this, signalCodec, serviceGroup, instanceId, index, messageTTLSeconds, autoAcknowledge, publisherConfirmationHandler);
      responseMessageRouters[index].initialize();
    }

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
   * Unique instance id used for whisper routing responses.
   *
   * @return responder instance id.
   */
  @Override
  public String getInstanceId () {

    return instanceId;
  }

  /**
   * Registers a service implementation with the invocation circuit.
   *
   * @param serviceInterface interface class for the service.
   * @param targetService    target implementation metadata.
   * @return this responder's instance id for whisper routing.
   * @throws Exception if registration fails.
   */
  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  /**
   * Creates a worker to handle incoming invocation messages.
   *
   * @param transferQueue queue supplying RabbitMQ messages.
   * @return new {@link InvocationWorker}.
   */
  @Override
  public InvocationWorker createWorker (WorkQueue<RabbitMQMessage> transferQueue) {

    return new InvocationWorker(transferQueue, this, invocationCircuit, signalCodec);
  }

  /**
   * @return current transport state.
   */
  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  /**
   * Resumes message consumption across routers.
   *
   * @throws Exception if a router cannot be started.
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
   * Pauses message consumption across routers.
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
   * Serializes and publishes a result to the caller's response queue.
   *
   * @param callerId      caller identifier.
   * @param correlationId correlation id matching the request.
   * @param error         whether the result represents an error.
   * @param nativeType    native type information.
   * @param result        payload to send.
   * @throws Throwable if publishing fails or the router pool is exhausted.
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
   * Closes routers and worker pool, preventing further message handling.
   *
   * @throws IOException          if closing a router fails.
   * @throws InterruptedException if shutdown is interrupted.
   * @throws TimeoutException     if closing a router times out.
   */
  @Override
  public void close ()
    throws IOException, InterruptedException, TimeoutException {

    if (closed.compareAndSet(false, true)) {
      synchronized (transportStateRef) {
        transportStateRef.set(TransportState.CLOSED);

        for (ResponseMessageRouter responseMessageRouter : responseMessageRouters) {
          responseMessageRouter.close();
        }

        shutDown();
      }
    }
  }
}
