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

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
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

/**
 * JMS-based request transport that supports all three vocal modes: talk (queue point-to-point),
 * whisper (topic addressed to a specific instance), and shout (topic broadcast).
 *
 * <p>Outbound requests are serialised as {@link InvocationSignal} payloads in JMS
 * {@link BytesMessage} objects and placed on the appropriate queue or topic.  Responses
 * arrive on a dedicated response topic, filtered by this transport's unique caller id, and
 * are decoded by a pool of {@link ResponseListener} instances.
 */
public class JmsRequestTransport extends AbstractRequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final SignalCodec signalCodec;
  private final LinkedBlockingQueue<MessageHandler> talkQueue;
  private final LinkedBlockingQueue<MessageHandler> whisperAndShoutQueue;
  private final ConnectionManager[] talkRequestConnectionManagers;
  private final ConnectionManager[] whisperAndShoutRequestConnectionManagers;
  private final ResponseListener[] responseListeners;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  /**
   * Constructs a JMS request transport, creating clustered connection managers, message-handler
   * pools, and response listeners.
   *
   * @param routingFactories      group of {@link ManagedObjectFactory} instances for request
   *                              queues, request topics, and response topics
   * @param messagePolicy         producer settings (delivery mode, TTL, priority, etc.)
   * @param reconnectionPolicy    reconnection timing and attempt limits
   * @param signalCodec           codec used to serialise {@link InvocationSignal} objects and
   *                              deserialise {@link org.smallmind.phalanx.wire.signal.ResultSignal} objects
   * @param clusterSize           number of independent connection managers to create per channel
   * @param concurrencyLimit      number of message-handler slots per channel pool
   * @param maximumMessageLength  maximum byte length of any single response payload
   * @param defaultTimeoutSeconds default request timeout in seconds when no explicit timeout is set
   * @throws JMSException       if any JMS resource cannot be created
   * @throws TransportException if transport initialisation fails
   */
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

  /**
   * Returns the snowflake-generated caller identifier that uniquely distinguishes this transport
   * instance and is used as a JMS message-selector on the response topic.
   *
   * @return unique caller id string
   */
  @Override
  public String getCallerId () {

    return callerId;
  }

  /**
   * Encodes the invocation as an {@link InvocationSignal}, sends it on the queue (talk mode)
   * or topic (whisper/shout mode), and returns the decoded result for two-way calls.
   *
   * @param voice     vocal-mode descriptor carrying conversation type and addressing info
   * @param route     target service route (service name, function, version)
   * @param arguments invocation arguments keyed by parameter name
   * @param contexts  optional wire context entries to propagate with the message
   * @return result object for request-reply conversations, or {@code null} for in-only calls
   * @throws Throwable if encoding, transmission, or result decoding fails
   */
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

      return Instrument.with(JmsRequestTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_RESULT.getDisplay())).on(
        () -> acquireResult(signalCodec, route, voice, messageId, inOnly)
      );
    } finally {
      messageQueue.put(messageHandler);
    }
  }

  /**
   * Polls the handler pool until a {@link MessageHandler} is available or the transport is closed.
   *
   * @param messageHandlerQueue pool from which to borrow a handler
   * @return a borrowed {@link MessageHandler}
   * @throws Throwable if the transport is closed before a handler becomes available
   */
  private MessageHandler acquireMessageHandler (final LinkedBlockingQueue<MessageHandler> messageHandlerQueue)
    throws Throwable {

    return Instrument.with(JmsRequestTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_REQUEST_TRANSPORT.getDisplay())).on(() -> {

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

  /**
   * Builds and populates a JMS {@link BytesMessage} for the given invocation parameters.
   *
   * @param messageHandler handler used to create the message
   * @param inOnly         {@code true} for fire-and-forget calls (caller id is not stamped)
   * @param serviceGroup   service group header value
   * @param instanceId     instance id header value, or {@code null} for non-whisper calls
   * @param route          target route header value
   * @param arguments      invocation arguments to encode
   * @param contexts       wire contexts to encode
   * @return populated JMS {@link Message} ready to send
   * @throws Throwable if the message cannot be created or encoded
   */
  private Message constructMessage (final MessageHandler messageHandler, final boolean inOnly, final String serviceGroup, final String instanceId, final Route route, final Map<String, Object> arguments, final WireContext... contexts)
    throws Throwable {

    return Instrument.with(JmsRequestTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

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

  /**
   * Stops and closes all request connection managers and response listeners.  Idempotent.
   *
   * @throws Exception if any resource cannot be stopped or closed
   */
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
