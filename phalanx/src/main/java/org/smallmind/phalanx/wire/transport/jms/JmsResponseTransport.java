/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import org.smallmind.claxon.registry.meter.LazyBuilder;
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

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  @Override
  public InvocationWorker createWorker (WorkQueue<Message> workQueue) {

    return new InvocationWorker(workQueue, this, invocationCircuit, signalCodec, maximumMessageLength);
  }

  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

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

  private Message constructMessage (final String callerId, final String correlationId, final TopicOperator topicOperator, final ResultSignal resultSignal)
    throws Throwable {

    return Instrument.with(InvocationWorker.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

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
