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
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.VocalMode;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.jms.QueueOperator;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Routes inbound requests to invocation workers and publishes responses over RabbitMQ.
 *
 * <p>Queue types are chosen by role rather than taken as configuration, because the correct type
 * follows from what the queue is for and a wrong choice is an outage rather than a preference. The
 * shout and whisper queues are per-instance and exclusive; the talk queue is shared by the service
 * group and is a quorum queue.
 */
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

  /**
   * Creates a response message router for the given service group and instance.
   *
   * @param connectionManager            owner of the connection this router's channel is taken from.
   * @param nameConfiguration            exchange and queue naming scheme.
   * @param responseTransport            owning transport, consulted for lifecycle state and dispatch.
   * @param signalCodec                  codec for serializing and deserializing signals.
   * @param serviceGroup                 service group name embedded in AMQP routing keys.
   * @param instanceId                   unique instance identifier used in shout and whisper queue names.
   * @param index                        ordinal index of this router, used in consumer tags.
   * @param ttlSeconds                   message time-to-live in seconds.
   * @param quorumReplicationCount       replica count for the shared talk queue; three or more, since a
   *                                     quorum queue needs a majority of its members and two replicas
   *                                     tolerate no failures at all.
   * @param autoAcknowledge              whether consumers should auto-ack delivered messages.
   * @param publisherConfirmationHandler optional handler for publisher confirms; may be {@code null}.
   */
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

  /**
   * Returns the name of this instance's shout queue.
   *
   * @return per-instance shout queue name.
   */
  public String getInstanceShoutQueueName () {

    return getShoutQueueName() + "-" + serviceGroup + "[" + instanceId + "]";
  }

  /**
   * Returns the name of the talk queue shared by the whole service group.
   *
   * @return shared talk queue name.
   */
  public String getGroupTalkQueueName () {

    return getTalkQueueName() + "-" + serviceGroup;
  }

  /**
   * Returns the name of this instance's whisper queue.
   *
   * @return per-instance whisper queue name.
   */
  public String getInstanceWhisperQueueName () {

    return getWhisperQueueName() + "-" + serviceGroup + "[" + instanceId + "]";
  }

  /**
   * Declares and binds the shout, talk and whisper queues, each independently, so that a queue the
   * broker will not hand over degrades this router rather than killing it.
   *
   * <p>The per-instance queues are exclusive. An exclusive queue belongs to the connection that
   * declared it and the broker deletes it the moment that connection ends, so it cannot survive as a
   * stale record whose process has stopped and which no client can then declare, delete or consume
   * from - the name is always free to be re-declared. Durability is inert for an exclusive queue and is
   * set only to stay clear of the broker's refusal to accept transient non-exclusive queues.
   * Auto-delete is off so that pausing, which cancels the consumers, does not destroy the queue and
   * drop whatever arrives meanwhile.
   *
   * <p>The talk queue is shared by the whole service group, so it can be neither exclusive nor
   * auto-delete. It is a quorum queue instead, which is what keeps it serving when a node is lost - but
   * only because the replication count is three.
   *
   * @throws IOException if the connection is unusable.
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

  /**
   * Resumes consumption on this router's queues.
   *
   * @throws IOException if installing a consumer fails.
   */
  public void play ()
    throws IOException {

    resumeConsumer();
  }

  /**
   * Suspends consumption by cancelling this router's consumers. The queues survive, so messages
   * arriving while paused are held rather than lost.
   *
   * @throws IOException if cancelling a consumer fails.
   */
  public void pause ()
    throws IOException {

    suspendConsumer();
  }

  /**
   * Installs consumers on the queues that actually bound. Idempotent: the consumer tag is claimed
   * before the broker call, so a rebuild and a resume arriving together cannot both install one, which
   * the broker would answer with a connection level error for reusing a consumer tag.
   *
   * @throws IOException if installing a consumer fails.
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

  /**
   * Cancels only the consumers this router actually installed. A basic.cancel against a tag the broker
   * has never seen is a channel error, which would turn a degraded router into a dead one.
   *
   * @throws IOException if cancelling fails.
   */
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

  /**
   * Returns whether this router should be consuming, which follows the owning transport's lifecycle
   * state. A transport that was paused stays paused across a rebuild - reinstalling consumers
   * unconditionally meant any channel bounce silently resumed a transport an operator had stopped.
   *
   * @return true if the transport is playing.
   */
  @Override
  public boolean isConsumerRequired () {

    return TransportState.PLAYING.equals(responseTransport.getState());
  }

  /**
   * Returns whether every queue this router managed to bind is being consumed. Derived from the bound
   * set rather than counted against a fixed number, so that changing which queues this router serves
   * cannot leave the health check quietly reporting on the wrong thing.
   *
   * @return true if each bound queue has a consumer, and at least one queue is bound.
   */
  @Override
  public boolean isConsumerInstalled () {

    return (!boundQueueSet.isEmpty()) && consumerTagMap.keySet().containsAll(boundQueueSet);
  }

  private void installConsumerInternal (Channel channel, String queueName)
    throws IOException {

    String consumerTag = queueName + "[" + index + "]";

    //  Idempotent, and claimed before the call rather than after it. A rebuild and a resume can both
    //  arrive here, and reusing a consumer tag is a connection level error, so it is worth being unable
    //  to do it twice by construction.
    if (consumerTagMap.putIfAbsent(queueName, consumerTag) == null) {
      try {
        channel.basicConsume(queueName, autoAcknowledge, consumerTag, false, false, null, new DefaultConsumer(channel) {

        @Override
        public synchronized void handleDelivery (String consumerTag, Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) {

          try {

            long timeInQueue = System.currentTimeMillis() - getTimestamp(properties);

            LoggerManager.getLogger(QueueOperator.class).debug("request message received(%s) in %d ms...", properties.getMessageId(), timeInQueue);
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
      } catch (IOException ioException) {
        //  The claim has to be released, or a later attempt on a healthy channel would skip this queue.
        consumerTagMap.remove(queueName);

        throw ioException;
      }
    }
  }

  /**
   * Serializes and publishes a result to the calling instance's response queue.
   *
   * @param callerId      identifier of the caller whose response queue should receive this.
   * @param correlationId correlation id matching the original request.
   * @param error         whether the result represents an error.
   * @param nativeType    native type information for the result.
   * @param result        payload to send.
   * @return the message id assigned to the published response.
   * @throws Throwable if encoding or publishing fails.
   */
  public String publish (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable {

    RabbitMQMessage rabbitMQMessage = constructMessage(correlationId, error, nativeType, result);

    send("response-" + callerId, getResponseExchangeName(), rabbitMQMessage.getProperties(), rabbitMQMessage.getBody());

    return rabbitMQMessage.getProperties().getMessageId();
  }

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

  /**
   * One of the queues this router serves - its name, whether it belongs to this instance alone, and how
   * it is declared and bound.
   */
  private static class RoutedQueue {

    private final ChannelOperation declaration;
    private final String queueName;
    private final boolean ephemeral;

    /**
     * Describes a queue this router serves.
     *
     * @param queueName   the queue's name.
     * @param ephemeral   true for a per-instance queue, whose name a new connection would free.
     * @param declaration the declaration and binding to perform.
     */
    private RoutedQueue (String queueName, boolean ephemeral, ChannelOperation declaration) {

      this.queueName = queueName;
      this.ephemeral = ephemeral;
      this.declaration = declaration;
    }

    /**
     * Returns the queue's name.
     *
     * @return queue name.
     */
    private String getQueueName () {

      return queueName;
    }

    /**
     * Returns whether the queue belongs to this instance alone.
     *
     * @return true if per-instance.
     */
    private boolean isEphemeral () {

      return ephemeral;
    }

    /**
     * Returns the declaration and binding to perform for this queue.
     *
     * @return the declaring operation.
     */
    private ChannelOperation getDeclaration () {

      return declaration;
    }
  }
}
