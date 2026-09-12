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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Publishes invocation signals to the request exchange and consumes correlated results from this
 * caller's own response queue, which is declared exclusive so that its name can never be left behind
 * as a record the caller cannot reclaim.
 */
public class RequestMessageRouter extends MessageRouter {

  private static final String CALLER_ID_AMQP_KEY = "x-opt-" + WireProperty.CALLER_ID.getKey();

  private final RabbitMQRequestTransport requestTransport;
  private final SignalCodec signalCodec;
  private final String callerId;
  private final AtomicReference<String> consumerTagRef = new AtomicReference<>();
  private final boolean autoAcknowledge;
  private final int index;
  private final int ttlSeconds;

  /**
   * Creates a request message router for the given caller.
   *
   * @param connectionManager            owner of the connection this router's channel is taken from.
   * @param nameConfiguration            exchange and queue naming scheme.
   * @param requestTransport             owning transport, notified when a correlated result arrives.
   * @param signalCodec                  codec for serializing and deserializing signals.
   * @param callerId                     unique caller identifier embedded in the response queue name.
   * @param index                        ordinal index of this router, used in the consumer tag.
   * @param ttlSeconds                   message time-to-live in seconds.
   * @param autoAcknowledge              whether the consumer should auto-ack delivered messages.
   * @param publisherConfirmationHandler optional handler for publisher confirms; may be {@code null}.
   */
  public RequestMessageRouter (RabbitMQConnectionManager connectionManager, NameConfiguration nameConfiguration, RabbitMQRequestTransport requestTransport, SignalCodec signalCodec, String callerId, int index, int ttlSeconds, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler) {

    super(connectionManager, "wire", nameConfiguration, publisherConfirmationHandler);

    this.requestTransport = requestTransport;
    this.signalCodec = signalCodec;
    this.callerId = callerId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
    this.autoAcknowledge = autoAcknowledge;
  }

  /**
   * Returns the name of this caller's response queue.
   *
   * @return per-caller response queue name.
   */
  public String getCallerResponseQueueName () {

    return getResponseQueueName() + "-" + callerId;
  }

  /**
   * Declares and binds this caller's response queue.
   *
   * <p>Exclusive, for the same reason as the per-instance queues on the responding side: the broker
   * deletes it with its connection, so the name can never be left behind as a record whose process has
   * been stopped. Durability is inert here and set only to stay clear of the broker's refusal to accept
   * transient non-exclusive queues; auto-delete is off because the connection governs its life.
   *
   * @throws IOException if the connection is unusable.
   */
  @Override
  public final void bindQueues ()
    throws IOException {

    markFullyBound(declareAndBind(getCallerResponseQueueName(), true, (channel) -> {

      channel.queueDeclare(getCallerResponseQueueName(), true, true, false, null);
      channel.queueBind(getCallerResponseQueueName(), getResponseExchangeName(), "response-" + callerId);
    }));
  }

  /**
   * Returns whether this router should be consuming, which for a caller is always.
   *
   * @return always true.
   */
  @Override
  public boolean isConsumerRequired () {

    return true;
  }

  /**
   * Returns whether the response queue consumer is installed.
   *
   * @return true if consuming.
   */
  @Override
  public boolean isConsumerInstalled () {

    return consumerTagRef.get() != null;
  }

  /**
   * Cancels the response queue consumer, if one was installed.
   *
   * @throws IOException if cancelling fails.
   */
  @Override
  public void uninstallConsumer ()
    throws IOException {

    operate((channel) -> {

      String consumerTag;

      if ((consumerTag = consumerTagRef.getAndSet(null)) != null) {
        try {
          channel.basicCancel(consumerTag);
        } catch (IOException ioException) {
          LoggerManager.getLogger(RequestMessageRouter.class).warn(ioException);
        }
      }
    });
  }

  /**
   * Installs the response queue consumer, provided the queue bound. Idempotent, since the consumer tag
   * is claimed before the broker call.
   *
   * @throws IOException if installing the consumer fails.
   */
  @Override
  public void installConsumer ()
    throws IOException {

    if (isFullyBound()) {
      operate((channel) -> {

        String consumerTag = getCallerResponseQueueName() + "[" + index + "]";

        channel.basicConsume(getCallerResponseQueueName(), autoAcknowledge, consumerTag, false, false, null, new DefaultConsumer(channel) {

          @Override
          public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

            try {

              long timeInTopic = System.currentTimeMillis() - getTimestamp(properties);

              LoggerManager.getLogger(ResponseMessageRouter.class).debug("response message received(%s) in %d ms...", properties.getMessageId(), timeInTopic);
              Instrument.with(RequestMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.RESPONSE_TRANSIT_TIME.getDisplay())).update((timeInTopic >= 0) ? timeInTopic : 0, TimeUnit.MILLISECONDS);

              Instrument.with(RequestMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.COMPLETE_CALLBACK.getDisplay())).on(
                () -> requestTransport.completeCallback(properties.getCorrelationId(), signalCodec.decode(body, 0, body.length, ResultSignal.class))
              );
            } catch (Throwable throwable) {
              LoggerManager.getLogger(ResponseMessageRouter.class).error(throwable);
            } finally {
              if (!autoAcknowledge) {
                try {
                  channel.basicAck(envelope.getDeliveryTag(), true);
                } catch (IOException ioException) {
                  LoggerManager.getLogger(ResponseMessageRouter.class).error(ioException);
                }
              }
            }
          }
        });

        consumerTagRef.set(consumerTag);
      });
    }
  }

  /**
   * Encodes and publishes an invocation signal, routed by the voice's vocal mode.
   *
   * @param inOnly       true for a fire-and-forget call, which carries no caller id for a reply.
   * @param serviceGroup service group to address.
   * @param voice        routing and conversation metadata, including the vocal mode.
   * @param route        target service, version and function.
   * @param arguments    named argument map to encode.
   * @param contexts     optional wire contexts propagated with the call.
   * @return the message id assigned to the published request.
   * @throws Throwable if encoding or publishing fails.
   */
  public String publish (final boolean inOnly, final String serviceGroup, final Voice<?, ?> voice, final Route route, final Map<String, Object> arguments, final WireContext... contexts)
    throws Throwable {

    RabbitMQMessage rabbitMQMessage = constructMessage(inOnly, route, arguments, contexts);
    StringBuilder routingKeyBuilder = new StringBuilder(voice.getMode().getName()).append("-").append(serviceGroup);

    if (voice.getMode().equals(VocalMode.WHISPER)) {
      routingKeyBuilder.append('[').append(voice.getInstanceId()).append(']');
    }

    send(routingKeyBuilder.toString(), getRequestExchangeName(), rabbitMQMessage.getProperties(), rabbitMQMessage.getBody());

    return rabbitMQMessage.getProperties().getMessageId();
  }

  private RabbitMQMessage constructMessage (final boolean inOnly, final Route route, final Map<String, Object> arguments, final WireContext... contexts)
    throws Throwable {

    return Instrument.with(RequestMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

      HashMap<String, Object> headerMap = new HashMap<>();

      if (!inOnly) {
        headerMap.put(CALLER_ID_AMQP_KEY, callerId);
      }

      AMQP.BasicProperties properties =
        new AMQP.BasicProperties.Builder()
          .contentType(signalCodec.getContentType())
          .messageId(SnowflakeId.newInstance().generateDottedString())
          .timestamp(new Date())
          .expiration(String.valueOf(ttlSeconds * 1000))
          .headers(headerMap).build();

      return new RabbitMQMessage(properties, signalCodec.encode(new InvocationSignal(inOnly, route, arguments, contexts)));
    });
  }
}
