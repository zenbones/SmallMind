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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.LazyBuilder;
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

public class RequestMessageRouter extends MessageRouter {

  private static final String CALLER_ID_AMQP_KEY = "x-opt-" + WireProperty.CALLER_ID.getKey();

  private final RabbitMQRequestTransport requestTransport;
  private final SignalCodec signalCodec;
  private final String callerId;
  private final boolean autoAcknowledge;
  private final int index;
  private final int ttlSeconds;

  public RequestMessageRouter (RabbitMQConnector connector, NameConfiguration nameConfiguration, RabbitMQRequestTransport requestTransport, SignalCodec signalCodec, String callerId, int index, int ttlSeconds, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler) {

    super(connector, "wire", nameConfiguration, publisherConfirmationHandler);

    this.requestTransport = requestTransport;
    this.signalCodec = signalCodec;
    this.callerId = callerId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
    this.autoAcknowledge = autoAcknowledge;
  }

  @Override
  public final void bindQueues ()
    throws IOException {

    operate((channel) -> {

      String queueName;

      channel.queueDeclare(queueName = getResponseQueueName() + "-" + callerId, false, false, true, null);
      channel.queueBind(queueName, getResponseExchangeName(), "response-" + callerId);
    });
  }

  @Override
  public void installConsumer ()
    throws IOException {

    operate((channel) -> {

      channel.basicConsume(getResponseQueueName() + "-" + callerId, autoAcknowledge, getResponseQueueName() + "-" + callerId + "[" + index + "]", false, false, null, new DefaultConsumer(channel) {

        @Override
        public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

          try {

            long timeInTopic = System.currentTimeMillis() - getTimestamp(properties);

            LoggerManager.getLogger(ResponseMessageRouter.class).debug("response message received(%s) in %d ms...", properties.getMessageId(), timeInTopic);
            Instrument.with(RequestMessageRouter.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.RESPONSE_TRANSIT_TIME.getDisplay())).update((timeInTopic >= 0) ? timeInTopic : 0, TimeUnit.MILLISECONDS);

            Instrument.with(RequestMessageRouter.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.COMPLETE_CALLBACK.getDisplay())).on(
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
    });
  }

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

    return Instrument.with(RequestMessageRouter.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

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
