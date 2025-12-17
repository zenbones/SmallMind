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
import java.util.concurrent.TimeUnit;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Routes incoming requests to invocation workers and publishes responses over RabbitMQ.
 */
public class ResponseMessageRouter extends MessageRouter {

  private final QueueContractor enduringQueueContractor;
  private final QueueContractor ephemeralQueueContractor;
  private final RabbitMQResponseTransport responseTransport;
  private final SignalCodec signalCodec;
  private final String serviceGroup;
  private final String instanceId;
  private final boolean autoAcknowledge;
  private final int index;
  private final int ttlSeconds;

  /**
   * @param connector                    connector for creating channels.
   * @param enduringQueueContractor      contractor for durable talk queues.
   * @param ephemeralQueueContractor     contractor for ephemeral whisper/shout queues.
   * @param nameConfiguration            exchange/queue naming scheme.
   * @param responseTransport            owning response transport.
   * @param signalCodec                  codec for serialization.
   * @param serviceGroup                 service group name used in routing keys.
   * @param instanceId                   unique id for whisper routing.
   * @param index                        router index for consumer tags.
   * @param ttlSeconds                   message TTL in seconds.
   * @param autoAcknowledge              whether consumers should auto-ack messages.
   * @param publisherConfirmationHandler optional handler for publisher confirms, may be null.
   */
  public ResponseMessageRouter (RabbitMQConnector connector, QueueContractor enduringQueueContractor, QueueContractor ephemeralQueueContractor, NameConfiguration nameConfiguration, RabbitMQResponseTransport responseTransport, SignalCodec signalCodec, String serviceGroup, String instanceId, int index, int ttlSeconds, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler) {

    super(connector, "wire", nameConfiguration, publisherConfirmationHandler);

    this.enduringQueueContractor = enduringQueueContractor;
    this.ephemeralQueueContractor = ephemeralQueueContractor;
    this.responseTransport = responseTransport;
    this.signalCodec = signalCodec;
    this.serviceGroup = serviceGroup;
    this.instanceId = instanceId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
    this.autoAcknowledge = autoAcknowledge;
  }

  /**
   * Declares and binds the shout, talk, and whisper request queues handled by this router.
   *
   * @throws IOException if queue declaration or binding fails.
   */
  @Override
  public void bindQueues ()
    throws IOException {

    operate((channel) -> {

      String shoutQueueName;
      String talkQueueName;
      String whisperQueueName;

      ephemeralQueueContractor.declare(channel, shoutQueueName = getShoutQueueName() + "-" + serviceGroup + "[" + instanceId + "]", true);
      channel.queueBind(shoutQueueName, getRequestExchangeName(), VocalMode.SHOUT.getName() + "-" + serviceGroup);

      enduringQueueContractor.declare(channel, talkQueueName = getTalkQueueName() + "-" + serviceGroup, false);
      channel.queueBind(talkQueueName, getRequestExchangeName(), VocalMode.TALK.getName() + "-" + serviceGroup);

      ephemeralQueueContractor.declare(channel, whisperQueueName = getWhisperQueueName() + "-" + serviceGroup + "[" + instanceId + "]", true);
      channel.queueBind(whisperQueueName, getRequestExchangeName(), VocalMode.WHISPER.getName() + "-" + serviceGroup + "[" + instanceId + "]");
    });
  }

  /**
   * Starts consumption on all routing queues.
   *
   * @throws IOException if installing a consumer fails.
   */
  public void play ()
    throws IOException {

    installConsumer();
  }

  /**
   * Stops consumption on all queues by canceling consumers.
   *
   * @throws IOException if canceling a consumer fails.
   */
  public void pause ()
    throws IOException {

    unInstallConsumer();
  }

  /**
   * Installs consumers for shout, talk, and whisper queues.
   *
   * @throws IOException if consumer installation fails.
   */
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

  /**
   * Cancels consumers for shout, talk, and whisper queues to stop message flow.
   *
   * @throws IOException if canceling consumers fails.
   */
  public void unInstallConsumer ()
    throws IOException {

    operate((channel) -> {

      channel.basicCancel(getShoutQueueName() + "-" + serviceGroup + "[" + instanceId + "]" + "[" + index + "]");
      channel.basicCancel(getTalkQueueName() + "-" + serviceGroup + "[" + index + "]");
      channel.basicCancel(getWhisperQueueName() + "-" + serviceGroup + "[" + instanceId + "]" + "[" + index + "]");
    });
  }

  /**
   * Installs a consumer on the supplied queue and wires it to execute responses.
   *
   * @param channel   channel used to install the consumer.
   * @param queueName queue to consume from.
   * @throws IOException if consumer installation fails.
   */
  private void installConsumerInternal (Channel channel, String queueName)
    throws IOException {

    channel.basicConsume(queueName, autoAcknowledge, queueName + "[" + index + "]", false, false, null, new DefaultConsumer(channel) {

      /**
       * Processes an inbound invocation request and forwards it for execution.
       */
      @Override
      public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

        try {

          long timeInQueue = System.currentTimeMillis() - getTimestamp(properties);

          LoggerManager.getLogger(ResponseMessageRouter.class).debug("request message received(%s) in %d ms...", properties.getMessageId(), timeInQueue);
          Instrument.with(ResponseMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.REQUEST_TRANSIT_TIME.getDisplay())).update((timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS);

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

  /**
   * Publishes a response message back to the caller over RabbitMQ.
   *
   * @param callerId      identifier of the requesting caller.
   * @param correlationId correlation id to match the originating request.
   * @param error         true if the result represents an error payload.
   * @param nativeType    native return type of the result payload.
   * @param result        encoded result payload.
   * @throws Throwable if message construction or publication fails.
   */
  public void publish (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable {

    RabbitMQMessage rabbitMQMessage = constructMessage(correlationId, error, nativeType, result);

    send("response-" + callerId, getResponseExchangeName(), rabbitMQMessage.getProperties(), rabbitMQMessage.getBody());
  }

  /**
   * Creates a response message with correlation and payload metadata.
   *
   * @param correlationId correlation id tying the response to the request.
   * @param error         whether the payload represents an error.
   * @param nativeType    result native type.
   * @param result        result payload.
   * @return response message ready for publication.
   * @throws Throwable if encoding fails.
   */
  private RabbitMQMessage constructMessage (final String correlationId, final boolean error, final String nativeType, final Object result)
    throws Throwable {

    return Instrument.with(ResponseMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(() -> {

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
