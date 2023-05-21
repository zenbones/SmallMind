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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.LazyBuilder;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.jms.QueueOperator;
import org.smallmind.scribe.pen.LoggerManager;

public class ResponseMessageRouter extends MessageRouter {

  private final RabbitMQResponseTransport responseTransport;
  private final SignalCodec signalCodec;
  private final String serviceGroup;
  private final String instanceId;
  private final boolean autoAcknowledge;
  private final int index;
  private final int ttlSeconds;

  public ResponseMessageRouter (RabbitMQConnector connector, NameConfiguration nameConfiguration, RabbitMQResponseTransport responseTransport, SignalCodec signalCodec, String serviceGroup, String instanceId, int index, int ttlSeconds, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler) {

    super(connector, nameConfiguration, publisherConfirmationHandler);

    this.responseTransport = responseTransport;
    this.signalCodec = signalCodec;
    this.serviceGroup = serviceGroup;
    this.instanceId = instanceId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
    this.autoAcknowledge = autoAcknowledge;
  }

  @Override
  public void bindQueues ()
    throws IOException {

    operate((channel) -> {

      String shoutQueueName;
      String talkQueueName;
      String whisperQueueName;

      channel.queueDeclare(shoutQueueName = getShoutQueueName() + "-" + serviceGroup + "[" + instanceId + "]", false, false, true, null);
      channel.queueBind(shoutQueueName, getRequestExchangeName(), VocalMode.SHOUT.getName() + "-" + serviceGroup);

      channel.queueDeclare(talkQueueName = getTalkQueueName() + "-" + serviceGroup, false, false, false, null);
      channel.queueBind(talkQueueName, getRequestExchangeName(), VocalMode.TALK.getName() + "-" + serviceGroup);

      channel.queueDeclare(whisperQueueName = getWhisperQueueName() + "-" + serviceGroup + "[" + instanceId + "]", false, false, true, null);
      channel.queueBind(whisperQueueName, getRequestExchangeName(), VocalMode.WHISPER.getName() + "-" + serviceGroup + "[" + instanceId + "]");
    });
  }

  public void play ()
    throws IOException {

    installConsumer();
  }

  public void pause ()
    throws IOException {

    unInstallConsumer();
  }

  @Override
  public void installConsumer ()
    throws IOException {

    operate((channel) -> {

      bindQueues();

      installConsumerInternal(channel, getShoutQueueName() + "-" + serviceGroup + "[" + instanceId + "]");
      installConsumerInternal(channel, getTalkQueueName() + "-" + serviceGroup);
      installConsumerInternal(channel, getWhisperQueueName() + "-" + serviceGroup + "[" + instanceId + "]");
    });
  }

  public void unInstallConsumer ()
    throws IOException {

    operate((channel) -> {

      channel.basicCancel(getShoutQueueName() + "-" + serviceGroup + "[" + instanceId + "]" + "[" + index + "]");
      channel.basicCancel(getTalkQueueName() + "-" + serviceGroup + "[" + index + "]");
      channel.basicCancel(getWhisperQueueName() + "-" + serviceGroup + "[" + instanceId + "]" + "[" + index + "]");
    });
  }

  private void installConsumerInternal (Channel channel, String queueName)
    throws IOException {

    channel.basicConsume(queueName, autoAcknowledge, queueName + "[" + index + "]", false, false, null, new DefaultConsumer(channel) {

      @Override
      public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

        try {

          long timeInQueue = System.currentTimeMillis() - getTimestamp(properties);

          LoggerManager.getLogger(QueueOperator.class).debug("request message received(%s) in %d ms...", properties.getMessageId(), timeInQueue);
          Instrument.with(ResponseMessageRouter.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.REQUEST_TRANSIT_TIME.getDisplay())).update((timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS);

          responseTransport.execute(new RabbitMQMessage(properties, body));
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
  }

  public String publish (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable {

    RabbitMQMessage rabbitMQMessage = constructMessage(correlationId, error, nativeType, result);

    send("response-" + callerId, getResponseExchangeName(), rabbitMQMessage.getProperties(), rabbitMQMessage.getBody());

    return rabbitMQMessage.getProperties().getMessageId();
  }

  private RabbitMQMessage constructMessage (final String correlationId, final boolean error, final String nativeType, final Object result)
    throws Throwable {

    return Instrument.with(ResponseMessageRouter.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

      AMQP.BasicProperties properties =
        new AMQP.BasicProperties.Builder()
          .contentType(signalCodec.getContentType())
          .messageId(SnowflakeId.newInstance().generateDottedString())
          .correlationId(correlationId)
          .timestamp(new Date())
          .expiration(String.valueOf(ttlSeconds * 1000 * 3)).build();

      return new RabbitMQMessage(properties, signalCodec.encode(new ResultSignal(error, nativeType, result)));
    });
  }
}
