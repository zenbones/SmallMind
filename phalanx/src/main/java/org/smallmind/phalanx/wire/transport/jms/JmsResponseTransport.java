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
package org.smallmind.phalanx.wire.transport.jms;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Topic;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.ServiceDefinitionException;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.phalanx.worker.WorkManager;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.WorkerFactory;

/**
 * JMS-based response transport that listens for incoming service invocation requests on queues
 * and topics, executes them through a pool of {@link InvocationWorker} instances, and publishes
 * encoded results back to callers via a response topic.
 *
 * <p>Three sets of {@link RequestListener} instances cover the three vocal modes:
 * <ul>
 *   <li><b>talk</b> – point-to-point queue listeners
 *   <li><b>shout</b> – topic listeners without instance filtering
 *   <li><b>whisper</b> – topic listeners filtered to this transport's unique instance id
 * </ul>
 */
public class JmsResponseTransport extends WorkManager<InvocationWorker, Message> implements WorkerFactory<InvocationWorker, Message>, ResponseTransport, ResponseTransmitter {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final SignalCodec signalCodec;
  private final ConcurrentLinkedQueue<TopicOperator> responseQueue;
  private final RequestListener[] shoutRequestListeners;
  private final RequestListener[] talkRequestListeners;
  private final RequestListener[] whisperRequestListeners;
  private final ConnectionManager[] responseConnectionManagers;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();
  private final int maximumMessageLength;

  /**
   * Constructs the response transport, establishing all request listeners and response publisher
   * pools, then starting the worker thread pool.
   *
   * @param routingFactories     group of {@link ManagedObjectFactory} instances for request queues,
   *                             request topics, and the response topic
   * @param messagePolicy        producer settings applied to all outbound message producers
   * @param reconnectionPolicy   reconnection timing and attempt limits for all connection managers
   * @param signalCodec          codec used to deserialise request signals and serialise result signals
   * @param serviceGroup         service group name used as a JMS message-selector filter
   * @param clusterSize          number of independent connection managers per channel
   * @param concurrencyLimit     maximum number of concurrent {@link InvocationWorker} threads
   * @param maximumMessageLength maximum byte length of any single inbound request payload
   * @throws InterruptedException if worker start-up is interrupted
   * @throws JMSException         if any JMS resource cannot be created
   * @throws TransportException   if transport initialisation fails
   */
  public JmsResponseTransport (RoutingFactories routingFactories, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, SignalCodec signalCodec, String serviceGroup, int clusterSize, int concurrencyLimit, int maximumMessageLength)
    throws InterruptedException, JMSException, TransportException {

    super(InvocationWorker.class, concurrencyLimit);

    int topicIndex = 0;

    this.signalCodec = signalCodec;
    this.maximumMessageLength = maximumMessageLength;

    shoutRequestListeners = new RequestListener[clusterSize];
    for (int index = 0; index < shoutRequestListeners.length; index++) {
      shoutRequestListeners[index] = new RequestListener(this, new ConnectionManager(routingFactories.getRequestTopicFactory(), messagePolicy, reconnectionPolicy), routingFactories.getRequestTopicFactory().getDestination(), serviceGroup, null);
    }
    talkRequestListeners = new RequestListener[clusterSize];
    for (int index = 0; index < talkRequestListeners.length; index++) {
      talkRequestListeners[index] = new RequestListener(this, new ConnectionManager(routingFactories.getRequestQueueFactory(), messagePolicy, reconnectionPolicy), routingFactories.getRequestQueueFactory().getDestination(), serviceGroup, null);
    }
    whisperRequestListeners = new RequestListener[clusterSize];
    for (int index = 0; index < whisperRequestListeners.length; index++) {
      whisperRequestListeners[index] = new RequestListener(this, new ConnectionManager(routingFactories.getRequestTopicFactory(), messagePolicy, reconnectionPolicy), routingFactories.getRequestTopicFactory().getDestination(), serviceGroup, instanceId);
    }

    responseConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < responseConnectionManagers.length; index++) {
      responseConnectionManagers[index] = new ConnectionManager(routingFactories.getResponseTopicFactory(), messagePolicy, reconnectionPolicy);
    }

    responseQueue = new ConcurrentLinkedQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      responseQueue.add(new TopicOperator(responseConnectionManagers[topicIndex], (Topic)routingFactories.getResponseTopicFactory().getDestination()));
      if (++topicIndex == responseConnectionManagers.length) {
        topicIndex = 0;
      }
    }

    startUp(this);
  }

  /**
   * Returns the snowflake-generated instance identifier used to distinguish this service
   * endpoint and to filter whisper-mode messages.
   *
   * @return unique instance id string
   */
  @Override
  public String getInstanceId () {

    return instanceId;
  }

  /**
   * Registers a service implementation against the given interface in the invocation circuit.
   *
   * @param serviceInterface the service interface whose invocations the target handles
   * @param targetService    the service implementation to register
   * @return the instance id of this transport
   * @throws NoSuchMethodException      if the interface declares methods the circuit cannot bind
   * @throws ServiceDefinitionException if registration violates service-definition constraints
   */
  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  /**
   * Creates an {@link InvocationWorker} bound to the given work queue.
   *
   * @param workQueue work queue from which the new worker draws messages
   * @return a new {@link InvocationWorker} instance
   */
  @Override
  public InvocationWorker createWorker (WorkQueue<Message> workQueue) {

    return new InvocationWorker(workQueue, this, invocationCircuit, signalCodec, maximumMessageLength);
  }

  /**
   * Returns the current transport state (playing, paused, or closed).
   *
   * @return current {@link TransportState}
   */
  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  /**
   * Transitions from {@link TransportState#PAUSED} to {@link TransportState#PLAYING} and
   * resumes message delivery on all request listeners.  No-op if already playing.
   *
   * @throws JMSException if any listener cannot be started
   */
  @Override
  public void play ()
    throws JMSException {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PAUSED, TransportState.PLAYING)) {
        for (RequestListener requestListener : shoutRequestListeners) {
          requestListener.play();
        }
        for (RequestListener requestListener : talkRequestListeners) {
          requestListener.play();
        }
        for (RequestListener requestListener : whisperRequestListeners) {
          requestListener.play();
        }
      }
    }
  }

  /**
   * Transitions from {@link TransportState#PLAYING} to {@link TransportState#PAUSED} and
   * suspends message delivery on all request listeners.  No-op if already paused.
   *
   * @throws JMSException if any listener cannot be stopped
   */
  @Override
  public void pause ()
    throws JMSException {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PLAYING, TransportState.PAUSED)) {
        for (RequestListener requestListener : shoutRequestListeners) {
          requestListener.pause();
        }
        for (RequestListener requestListener : talkRequestListeners) {
          requestListener.pause();
        }
        for (RequestListener requestListener : whisperRequestListeners) {
          requestListener.pause();
        }
      }
    }
  }

  /**
   * Encodes a {@link ResultSignal} and publishes it to the response topic so the originating
   * caller can complete its pending callback.
   *
   * @param callerId      id of the caller that issued the original request
   * @param correlationId JMS correlation id matching the request message id
   * @param error         {@code true} if the result represents a fault
   * @param nativeType    fully-qualified type name of the result object, or {@code null}
   * @param result        result value to encode and transmit
   * @throws Throwable if a topic operator is unavailable or encoding/sending fails
   */
  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable {

    TopicOperator topicOperator;

    if ((topicOperator = responseQueue.poll()) == null) {
      throw new TransportException("Unable to take a TopicOperator, which should never happen - please contact your system administrator");
    }

    topicOperator.send(constructMessage(callerId, correlationId, topicOperator, new ResultSignal(error, nativeType, result)));
    responseQueue.add(topicOperator);
  }

  /**
   * Builds a JMS {@link BytesMessage} carrying the encoded result signal and the required
   * correlation and routing headers.
   *
   * @param callerId      caller id stamped as a JMS message property
   * @param correlationId JMS correlation id linking the response to the original request
   * @param topicOperator operator used to create the message
   * @param resultSignal  result signal to encode into the message body
   * @return a populated response {@link Message} ready to send
   * @throws Throwable if message creation or encoding fails
   */
  private Message constructMessage (final String callerId, final String correlationId, final TopicOperator topicOperator, final ResultSignal resultSignal)
    throws Throwable {

    return Instrument.with(InvocationWorker.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

      BytesMessage responseMessage;

      responseMessage = topicOperator.createMessage();

      responseMessage.writeBytes(signalCodec.encode(resultSignal));

      responseMessage.setJMSCorrelationID(correlationId);
      responseMessage.setStringProperty(WireProperty.CALLER_ID.getKey(), callerId);
      responseMessage.setStringProperty(WireProperty.CONTENT_TYPE.getKey(), signalCodec.getContentType());
      responseMessage.setLongProperty(WireProperty.CLOCK.getKey(), System.currentTimeMillis());

      return responseMessage;
    });
  }

  /**
   * Closes all request listeners and response connection managers, then shuts down the worker
   * pool.  Sets the transport state to {@link TransportState#CLOSED}.  Idempotent.
   *
   * @throws JMSException         if any JMS resource cannot be closed
   * @throws InterruptedException if the worker shutdown is interrupted
   */
  @Override
  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      synchronized (transportStateRef) {
        transportStateRef.set(TransportState.CLOSED);

        for (RequestListener requestListener : shoutRequestListeners) {
          requestListener.close();
        }
        for (RequestListener requestListener : talkRequestListeners) {
          requestListener.close();
        }
        for (RequestListener requestListener : whisperRequestListeners) {
          requestListener.close();
        }

        for (ConnectionManager responseConnectionManager : responseConnectionManagers) {
          responseConnectionManager.stop();
        }
        for (ConnectionManager responseConnectionManager : responseConnectionManagers) {
          responseConnectionManager.close();
        }

        shutDown();
      }
    }
  }
}
