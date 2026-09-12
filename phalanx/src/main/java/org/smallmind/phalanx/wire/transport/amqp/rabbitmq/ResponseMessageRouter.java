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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
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
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.jms.QueueOperator;
import org.smallmind.scribe.pen.LoggerManager;

public class ResponseMessageRouter extends MessageRouter {

  private final ConcurrentHashMap<String, String> consumerTagMap = new ConcurrentHashMap<>();
  private final CopyOnWriteArraySet<String> boundQueueSet = new CopyOnWriteArraySet<>();
  private final RabbitMQResponseTransport responseTransport;
  private final SignalCodec signalCodec;
  private final String serviceGroup;
  private final String instanceId;
  private final boolean autoAcknowledge;
  private final int index;
  private final int ttlSeconds;
  private final int quorumReplicationCount;

  public ResponseMessageRouter (RabbitMQConnectionManager connectionManager, NameConfiguration nameConfiguration, RabbitMQResponseTransport responseTransport, SignalCodec signalCodec, String serviceGroup, String instanceId, int index, int ttlSeconds, int quorumReplicationCount, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler) {

    super(connectionManager, "wire", nameConfiguration, publisherConfirmationHandler);

    this.responseTransport = responseTransport;
    this.signalCodec = signalCodec;
    this.serviceGroup = serviceGroup;
    this.instanceId = instanceId;
    this.index = index;
    this.ttlSeconds = ttlSeconds;
    this.quorumReplicationCount = quorumReplicationCount;
    this.autoAcknowledge = autoAcknowledge;
  }

  public String getInstanceShoutQueueName () {

    return getShoutQueueName() + "-" + serviceGroup + "[" + instanceId + "]";
  }

  public String getGroupTalkQueueName () {

    return getTalkQueueName() + "-" + serviceGroup;
  }

  public String getInstanceWhisperQueueName () {

    return getWhisperQueueName() + "-" + serviceGroup + "[" + instanceId + "]";
  }

  /*
   * Each queue is declared independently, so a queue the broker will not hand over degrades this
   * router rather than killing it.
   *
   * The per-instance queues are exclusive. An exclusive queue belongs to the connection that declared
   * it and the broker deletes it the moment that connection ends, so it cannot survive as a stale
   * record the way the durable non-exclusive queue in the incident did - the name is always free to be
   * re-declared. Durability is inert for an exclusive queue and is set only to stay clear of the
   * broker's refusal to accept transient non-exclusive queues. Auto-delete is off so that pausing,
   * which cancels the consumers, no longer destroys the queue and drops whatever arrives meanwhile.
   *
   * The talk queue is shared by the whole service group, so it can be neither exclusive nor
   * auto-delete. It is a quorum queue instead, which is what keeps it serving when a node is lost -
   * but only because the replication count is three. Two replicas tolerate no failures at all.
   */
  @Override
  public void bindQueues ()
    throws IOException {

    boolean fullyBound = true;

    consumerTagMap.clear();
    boundQueueSet.clear();

    for (RoutedQueue routedQueue : getRoutedQueues()) {
      if (declareAndBind(routedQueue.getQueueName(), routedQueue.isEphemeral(), routedQueue.getDeclaration())) {
        boundQueueSet.add(routedQueue.getQueueName());
      } else {
        fullyBound = false;
      }
    }

    markFullyBound(fullyBound);
  }

  /*
   * The single definition of which queues this router serves, how each is declared, and which of them
   * are per-instance. Binding, consuming and the consumer health check all iterate this, so adding or
   * removing a queue is one edit here rather than three that have to agree.
   */
  private RoutedQueue[] getRoutedQueues () {

    return new RoutedQueue[] {
      new RoutedQueue(getInstanceShoutQueueName(), true, (channel) -> {

        channel.queueDeclare(getInstanceShoutQueueName(), true, true, false, null);
        channel.queueBind(getInstanceShoutQueueName(), getRequestExchangeName(), VocalMode.SHOUT.getName() + "-" + serviceGroup);
      }),
      new RoutedQueue(getGroupTalkQueueName(), false, (channel) -> {

        channel.queueDeclare(getGroupTalkQueueName(), true, false, false, Map.of("x-queue-type", "quorum", "x-quorum-initial-group-size", quorumReplicationCount));
        channel.queueBind(getGroupTalkQueueName(), getRequestExchangeName(), VocalMode.TALK.getName() + "-" + serviceGroup);
      }),
      new RoutedQueue(getInstanceWhisperQueueName(), true, (channel) -> {

        channel.queueDeclare(getInstanceWhisperQueueName(), true, true, false, null);
        channel.queueBind(getInstanceWhisperQueueName(), getRequestExchangeName(), VocalMode.WHISPER.getName() + "-" + serviceGroup + "[" + instanceId + "]");
      })
    };
  }

  public void play ()
    throws IOException {

    resumeConsumer();
  }

  public void pause ()
    throws IOException {

    suspendConsumer();
  }

  /*
   * Only queues that actually bound get a consumer, and only consumers that were actually installed
   * are ever cancelled - basic.cancel against a tag the broker has never seen is a channel error, which
   * would turn a degraded router into a dead one.
   */
  @Override
  public void installConsumer ()
    throws IOException {

    operate((channel) -> {

      for (String queueName : boundQueueSet) {
        installConsumerInternal(channel, queueName);
      }
    });
  }

  @Override
  public void uninstallConsumer ()
    throws IOException {

    operate((channel) -> {

      for (String queueName : consumerTagMap.keySet()) {

        String consumerTag;

        if ((consumerTag = consumerTagMap.remove(queueName)) != null) {
          try {
            channel.basicCancel(consumerTag);
          } catch (IOException ioException) {
            LoggerManager.getLogger(ResponseMessageRouter.class).warn(ioException);
          }
        }
      }
    });
  }

  /*
   * A transport that was paused stays paused across a rebuild. Reinstalling consumers unconditionally
   * meant any channel bounce silently resumed a transport an operator had deliberately stopped.
   */
  @Override
  public boolean isConsumerRequired () {

    return TransportState.PLAYING.equals(responseTransport.getState());
  }

  @Override
  public boolean isConsumerInstalled () {

    return (!boundQueueSet.isEmpty()) && consumerTagMap.keySet().containsAll(boundQueueSet);
  }

  private void installConsumerInternal (Channel channel, String queueName)
    throws IOException {

    String consumerTag = queueName + "[" + index + "]";

    //  Idempotent, and claimed before the call rather than after it. Belt and braces alongside the
    //  monitor in resumeConsumer - reusing a consumer tag is a connection level error, so it is worth
    //  being unable to do it twice by construction.
    if (consumerTagMap.putIfAbsent(queueName, consumerTag) == null) {
      try {
        channel.basicConsume(queueName, autoAcknowledge, consumerTag, false, false, null, new DefaultConsumer(channel) {

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
      } catch (IOException ioException) {
        //  The claim has to be released, or a later attempt on a healthy channel would skip this queue.
        consumerTagMap.remove(queueName);

        throw ioException;
      }
    }
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

  /*
   * One of the queues this router serves - its name, whether it belongs to this instance alone, and how
   * it is declared and bound.
   */
  private static class RoutedQueue {

    private final ChannelOperation declaration;
    private final String queueName;
    private final boolean ephemeral;

    private RoutedQueue (String queueName, boolean ephemeral, ChannelOperation declaration) {

      this.queueName = queueName;
      this.ephemeral = ephemeral;
      this.declaration = declaration;
    }

    private String getQueueName () {

      return queueName;
    }

    private boolean isEphemeral () {

      return ephemeral;
    }

    private ChannelOperation getDeclaration () {

      return declaration;
    }
  }
}
