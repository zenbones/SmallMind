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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Base class for routing AMQP messages, handling connection/channel lifecycle, binding queues, and publishing.
 */
public abstract class MessageRouter {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicStampedReference<ConnectionAndChannel> connectionAndChannelRef = new AtomicStampedReference<>(null, 0);
  private final AtomicInteger version = new AtomicInteger(0);
  private final RabbitMQConnector connector;
  private final NameConfiguration nameConfiguration;
  private final PublisherConfirmationHandler publisherConfirmationHandler;
  private final String prefix;

  /**
   * @param connector                    provider for AMQP connections.
   * @param prefix                       prefix applied to exchange/queue names.
   * @param nameConfiguration            configuration of exchange/queue suffixes.
   * @param publisherConfirmationHandler handler for publisher confirms, or {@code null} if not used.
   */
  public MessageRouter (RabbitMQConnector connector, String prefix, NameConfiguration nameConfiguration, PublisherConfirmationHandler publisherConfirmationHandler) {

    this.connector = connector;
    this.prefix = prefix;
    this.nameConfiguration = nameConfiguration;
    this.publisherConfirmationHandler = publisherConfirmationHandler;
  }

  public abstract void bindQueues ()
    throws IOException;

  public abstract void installConsumer ()
    throws IOException;

  /**
   * Initializes the router by creating channel, exchanges, queues, and consumers.
   *
   * @throws IOException      if AMQP operations fail.
   * @throws TimeoutException if creating a channel times out.
   */
  public void initialize ()
    throws IOException, TimeoutException {

    ensureChannel(0);
  }

  /**
   * @return fully qualified request exchange name.
   */
  public String getRequestExchangeName () {

    return prefix + "-" + nameConfiguration.getRequestExchange();
  }

  /**
   * @return fully qualified response exchange name.
   */
  public String getResponseExchangeName () {

    return prefix + "-" + nameConfiguration.getResponseExchange();
  }

  /**
   * @return fully qualified response queue name.
   */
  public String getResponseQueueName () {

    return prefix + "-" + nameConfiguration.getResponseQueue();
  }

  /**
   * @return fully qualified shout queue name.
   */
  public String getShoutQueueName () {

    return prefix + "-" + nameConfiguration.getShoutQueue();
  }

  /**
   * @return fully qualified talk queue name.
   */
  public String getTalkQueueName () {

    return prefix + "-" + nameConfiguration.getTalkQueue();
  }

  /**
   * @return fully qualified whisper queue name.
   */
  public String getWhisperQueueName () {

    return prefix + "-" + nameConfiguration.getWhisperQueue();
  }

  /**
   * Ensures there is an open channel/connection pair, replacing it if needed.
   *
   * @param stamp expected version stamp for the current channel.
   * @throws IOException      if AMQP operations fail.
   * @throws TimeoutException if channel creation times out.
   */
  private void ensureChannel (int stamp)
    throws IOException, TimeoutException {

    synchronized (connectionAndChannelRef) {
      if (!closed.get()) {
        if (connectionAndChannelRef.getStamp() == stamp) {

          ConnectionAndChannel previousConnectionAndChannel;
          Connection connection;
          Channel channel;
          final int nextStamp;

          if ((previousConnectionAndChannel = connectionAndChannelRef.getReference()) != null) {
            if (previousConnectionAndChannel.getConnection().isOpen()) {
              try {
                previousConnectionAndChannel.close();
              } catch (IOException ioException) {
                LoggerManager.getLogger(MessageRouter.class).error(ioException);
              }
            }
          }

          if ((channel = (connection = connector.getConnection()).createChannel()) == null) {
            throw new IOException("No channel is available");
          } else {

            if (publisherConfirmationHandler != null) {
              channel.confirmSelect();
              channel.addConfirmListener(publisherConfirmationHandler.generateConfirmListener());
            }

            channel.basicQos(0, 1, false);
            channel.exchangeDeclare(getRequestExchangeName(), "direct", false, false, null);
            channel.exchangeDeclare(getResponseExchangeName(), "direct", false, false, null);

            connectionAndChannelRef.set(new ConnectionAndChannel(connection, channel), nextStamp = version.incrementAndGet());
            channel.addShutdownListener((cause) -> {

              try {
                if (!closed.get()) {
                  ensureChannel(nextStamp);
                }
              } catch (IOException | TimeoutException exception) {
                LoggerManager.getLogger(MessageRouter.class).error(exception);
              }
            });

            bindQueues();
            installConsumer();
          }
        }
      }
    }
  }

  /**
   * Runs a channel operation while holding the channel lock to prevent concurrent modification.
   *
   * @param channelOperation operation to execute with the current channel.
   * @throws IOException if the channel operation throws.
   */
  public void operate (ChannelOperation channelOperation)
    throws IOException {

    synchronized (connectionAndChannelRef) {

      channelOperation.execute(connectionAndChannelRef.getReference().getChannel());
    }
  }

  /**
   * Publishes a message with the provided routing key, retrying if the channel closes.
   *
   * @param routingKey   binding key for the target queue.
   * @param exchangeName exchange to publish to.
   * @param properties   message properties.
   * @param body         message payload.
   * @throws IOException      if publish fails.
   * @throws TimeoutException if establishing a new channel times out.
   */
  public void send (String routingKey, String exchangeName, AMQP.BasicProperties properties, byte[] body)
    throws IOException, TimeoutException {

    if (!closed.get()) {

      boolean sent = false;

      do {

        int[] stampHolder = new int[1];
        Channel channel = connectionAndChannelRef.get(stampHolder).getChannel();

        try {
          channel.basicPublish(exchangeName, routingKey, true, false, properties, body);
          sent = true;
        } catch (AlreadyClosedException exception) {
          ensureChannel(stampHolder[0]);
        }
      } while ((!sent) && (!closed.get()));
    }
  }

  /**
   * Extracts the timestamp property as milliseconds since epoch, or {@link Long#MAX_VALUE} when absent.
   *
   * @param properties message properties to examine.
   * @return milliseconds since epoch of the timestamp header, or {@link Long#MAX_VALUE} if missing.
   */
  public long getTimestamp (AMQP.BasicProperties properties) {

    Date date;

    if ((date = properties.getTimestamp()) != null) {

      return date.getTime();
    }

    return Long.MAX_VALUE;
  }

  /**
   * Closes the channel and connection if open, preventing further routing.
   *
   * @throws IOException      if closing the connection fails.
   * @throws TimeoutException if closing the channel times out.
   */
  public void close ()
    throws IOException, TimeoutException {

    if (closed.compareAndSet(false, true)) {
      synchronized (connectionAndChannelRef) {

        ConnectionAndChannel connectionAndChannel;

        if ((connectionAndChannel = connectionAndChannelRef.getReference()) != null) {
          connectionAndChannel.close();
        }
      }
    }
  }

  private static class ConnectionAndChannel {

    private final Connection connection;
    private final Channel channel;

    /**
     * @param connection the owning AMQP connection.
     * @param channel    channel created from the connection.
     */
    public ConnectionAndChannel (Connection connection, Channel channel) {

      this.connection = connection;
      this.channel = channel;
    }

    /**
     * @return the owning AMQP connection.
     */
    public Connection getConnection () {

      return connection;
    }

    /**
     * @return the channel created from the connection.
     */
    public Channel getChannel () {

      return channel;
    }

    /**
     * Closes the underlying connection (and channel with it).
     *
     * @throws IOException if the connection cannot be closed cleanly.
     */
    public void close ()
      throws IOException {

      connection.close();
    }
  }
}
