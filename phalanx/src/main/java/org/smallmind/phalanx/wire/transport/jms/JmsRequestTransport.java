/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.LazyBuilder;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.ConversationType;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.transport.AbstractRequestTransport;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.WireProperty;

public class JmsRequestTransport extends AbstractRequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final SignalCodec signalCodec;
  private final LinkedBlockingQueue<MessageHandler> talkQueue;
  private final LinkedBlockingQueue<MessageHandler> whisperAndShoutQueue;
  private final ConnectionManager[] talkRequestConnectionManagers;
  private final ConnectionManager[] whisperAndShoutRequestConnectionManagers;
  private final ResponseListener[] responseListeners;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  public JmsRequestTransport (RoutingFactories routingFactories, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, int maximumMessageLength, long defaultTimeoutSeconds)
    throws JMSException, TransportException {

    super(defaultTimeoutSeconds);

    int talkIndex = 0;
    int whisperIndex = 0;

    this.signalCodec = signalCodec;

    talkRequestConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < talkRequestConnectionManagers.length; index++) {
      talkRequestConnectionManagers[index] = new ConnectionManager(routingFactories.getRequestQueueFactory(), messagePolicy, reconnectionPolicy);
    }
    whisperAndShoutRequestConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < whisperAndShoutRequestConnectionManagers.length; index++) {
      whisperAndShoutRequestConnectionManagers[index] = new ConnectionManager(routingFactories.getRequestTopicFactory(), messagePolicy, reconnectionPolicy);
    }

    talkQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      talkQueue.add(new QueueOperator(talkRequestConnectionManagers[talkIndex], (Queue)routingFactories.getRequestQueueFactory().getDestination()));
      if (++talkIndex == talkRequestConnectionManagers.length) {
        talkIndex = 0;
      }
    }
    whisperAndShoutQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      whisperAndShoutQueue.add(new TopicOperator(whisperAndShoutRequestConnectionManagers[whisperIndex], (Topic)routingFactories.getRequestTopicFactory().getDestination()));
      if (++whisperIndex == whisperAndShoutRequestConnectionManagers.length) {
        whisperIndex = 0;
      }
    }

    responseListeners = new ResponseListener[clusterSize];
    for (int index = 0; index < responseListeners.length; index++) {
      responseListeners[index] = new ResponseListener(this, new ConnectionManager(routingFactories.getResponseTopicFactory(), messagePolicy, reconnectionPolicy), (Topic)routingFactories.getResponseTopicFactory().getDestination(), signalCodec, callerId, maximumMessageLength);
    }
  }

  @Override
  public String getCallerId () {

    return callerId;
  }

  @Override
  public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    LinkedBlockingQueue<MessageHandler> messageQueue = voice.getMode().equals(VocalMode.TALK) ? talkQueue : whisperAndShoutQueue;
    final MessageHandler messageHandler = acquireMessageHandler(messageQueue);
    boolean inOnly = voice.getConversation().getConversationType().equals(ConversationType.IN_ONLY);

    try {

      Message requestMessage;
      String messageId;

      messageHandler.send(requestMessage = constructMessage(messageHandler, inOnly, (String)voice.getServiceGroup(), voice.getMode().equals(VocalMode.WHISPER) ? (String)voice.getInstanceId() : null, route, arguments, contexts));
      messageId = requestMessage.getJMSMessageID();

      return Instrument.with(JmsRequestTransport.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_RESULT.getDisplay())).on(
        () -> acquireResult(signalCodec, route, voice, messageId, inOnly)
      );
    } finally {
      messageQueue.put(messageHandler);
    }
  }

  private MessageHandler acquireMessageHandler (final LinkedBlockingQueue<MessageHandler> messageHandlerQueue)
    throws Throwable {

    return Instrument.with(JmsRequestTransport.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_REQUEST_TRANSPORT.getDisplay())).on(() -> {

      MessageHandler messageHandler;

      do {
        messageHandler = messageHandlerQueue.poll(1, TimeUnit.SECONDS);
      } while ((!closed.get()) && (messageHandler == null));

      if (messageHandler == null) {
        throw new TransportException("Message transmission has been closed");
      }

      return messageHandler;
    });
  }

  private Message constructMessage (final MessageHandler messageHandler, final boolean inOnly, final String serviceGroup, final String instanceId, final Route route, final Map<String, Object> arguments, final WireContext... contexts)
    throws Throwable {

    return Instrument.with(JmsRequestTransport.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

      BytesMessage requestMessage;

      requestMessage = messageHandler.createMessage();

      requestMessage.writeBytes(signalCodec.encode(new InvocationSignal(inOnly, route, arguments, contexts)));

      if (!inOnly) {
        requestMessage.setStringProperty(WireProperty.CALLER_ID.getKey(), callerId);
      }

      requestMessage.setStringProperty(WireProperty.CONTENT_TYPE.getKey(), signalCodec.getContentType());
      requestMessage.setLongProperty(WireProperty.CLOCK.getKey(), System.currentTimeMillis());
      requestMessage.setStringProperty(WireProperty.SERVICE_GROUP.getKey(), serviceGroup);

      if (instanceId != null) {
        requestMessage.setStringProperty(WireProperty.INSTANCE_ID.getKey(), instanceId);
      }

      return requestMessage;
    });
  }

  @Override
  public void close ()
    throws Exception {

    if (closed.compareAndSet(false, true)) {
      for (ConnectionManager requestConnectionManager : whisperAndShoutRequestConnectionManagers) {
        requestConnectionManager.stop();
      }
      for (ConnectionManager requestConnectionManager : talkRequestConnectionManagers) {
        requestConnectionManager.stop();
      }

      for (ConnectionManager requestConnectionManager : whisperAndShoutRequestConnectionManagers) {
        requestConnectionManager.close();
      }
      for (ConnectionManager requestConnectionManager : talkRequestConnectionManagers) {
        requestConnectionManager.close();
      }

      for (ResponseListener responseListener : responseListeners) {
        responseListener.close();
      }
    }
  }
}
