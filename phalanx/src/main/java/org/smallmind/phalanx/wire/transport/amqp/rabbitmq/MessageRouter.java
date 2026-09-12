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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownSignalException;
import org.smallmind.nutsnbolts.lang.FormattedIOException;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class MessageRouter {

  private static final long CHANNEL_WAIT_MILLISECONDS = 30000;

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean fullyBound = new AtomicBoolean(false);
  private final AtomicBoolean connectionReplacementRequested = new AtomicBoolean(false);
  private final AtomicStampedReference<Channel> channelRef = new AtomicStampedReference<>(null, 0);
  private final AtomicInteger channelFailureCount = new AtomicInteger(0);
  private final RabbitMQConnectionManager connectionManager;
  private final NameConfiguration nameConfiguration;
  private final PublisherConfirmationHandler publisherConfirmationHandler;
  private final String prefix;

  public MessageRouter (RabbitMQConnectionManager connectionManager, String prefix, NameConfiguration nameConfiguration, PublisherConfirmationHandler publisherConfirmationHandler) {

    this.connectionManager = connectionManager;
    this.prefix = prefix;
    this.nameConfiguration = nameConfiguration;
    this.publisherConfirmationHandler = publisherConfirmationHandler;

    connectionManager.register(this);
  }

  public abstract void bindQueues ()
    throws IOException;

  public abstract void installConsumer ()
    throws IOException;

  public abstract void uninstallConsumer ()
    throws IOException;

  public abstract boolean isConsumerRequired ();

  public abstract boolean isConsumerInstalled ();

  public String getRequestExchangeName () {

    return prefix + "-" + nameConfiguration.getRequestExchange();
  }

  public String getResponseExchangeName () {

    return prefix + "-" + nameConfiguration.getResponseExchange();
  }

  public String getResponseQueueName () {

    return prefix + "-" + nameConfiguration.getResponseQueue();
  }

  public String getShoutQueueName () {

    return prefix + "-" + nameConfiguration.getShoutQueue();
  }

  public String getTalkQueueName () {

    return prefix + "-" + nameConfiguration.getTalkQueue();
  }

  public String getWhisperQueueName () {

    return prefix + "-" + nameConfiguration.getWhisperQueue();
  }

  /*
   * Builds this router's channel on the connection the manager currently owns. Called only from the
   * manager, which serializes rebuilds and owns every retry decision - this method never schedules
   * anything itself, and never swallows a connection level failure.
   */
  public void rebuild (Connection connection, int generation)
    throws IOException {

    synchronized (channelRef) {
      if (!closed.get()) {

        Channel channel;

        discardChannel();
        connectionReplacementRequested.set(false);
        fullyBound.set(false);

        if ((channel = connection.createChannel()) == null) {
          throw new FormattedIOException("No channel is available");
        } else {
          if (publisherConfirmationHandler != null) {
            channel.confirmSelect();
            channel.addConfirmListener(publisherConfirmationHandler.generateConfirmListener());
          }

          channel.basicQos(0, 1, false);
          channel.exchangeDeclare(getRequestExchangeName(), "direct", false, false, null);
          channel.exchangeDeclare(getResponseExchangeName(), "direct", false, false, null);

          channelRef.set(channel, generation);
          channel.addShutdownListener((cause) -> {

            if ((!closed.get()) && (!cause.isInitiatedByApplication())) {
              connectionManager.onChannelFailure(this, generation, cause);
            }
          });

          bindQueues();

          if (isConsumerRequired()) {
            installConsumer();
          }
        }
      }
    }
  }

  /*
   * Declares and binds a single queue on a channel of its own, so that one queue the broker will not
   * hand over cannot abort the declaration of the others. Returns false on a channel level refusal,
   * which leaves the router degraded but serving whatever did bind. A connection level failure is
   * rethrown, because nothing on this connection is salvageable.
   */
  protected boolean declareAndBind (String queueName, boolean ephemeral, ChannelOperation channelOperation)
    throws IOException {

    Connection connection;

    if ((connection = connectionManager.getConnection()) == null) {
      throw new FormattedIOException("No connection is available");
    }

    try {

      Channel declarationChannel = connection.createChannel();

      try {
        channelOperation.execute(declarationChannel);
      } finally {
        try {
          declarationChannel.close();
        } catch (Exception exception) {
          //  the declaration either took or did not, and a close failure tells us nothing more
        }
      }

      return true;
    } catch (Exception exception) {

      ShutdownSignalException shutdownSignalException;

      if (((shutdownSignalException = asShutdownSignal(exception)) == null) || shutdownSignalException.isHardError()) {
        if (exception instanceof IOException) {

          throw (IOException)exception;
        }

        throw new FormattedIOException(exception);
      }

      if (isNotFound(shutdownSignalException) && ephemeral) {
        //  The incident signature. A per-instance queue whose process the broker has stopped cannot be
        //  declared, deleted (that answers 541 and takes the connection with it) or consumed from. Only
        //  dropping the connection frees the name, which is exactly what an exclusive queue guarantees.
        LoggerManager.getLogger(MessageRouter.class).error("queue(%s) reports a stopped process - requesting a connection replacement to force its re-creation", queueName);
        connectionReplacementRequested.set(true);
      } else {
        LoggerManager.getLogger(MessageRouter.class).warn("unable to declare queue(%s) - %s", queueName, shutdownSignalException.getReason());
      }

      return false;
    }
  }

  /*
   * A transport resumed while the manager happens to be rebuilding can install consumers twice on one
   * channel - the rebuild installs because the state now reads PLAYING, and play() installs again -
   * and the broker answers 530 NOT_ALLOWED, reuse of a consumer tag, which is a connection level
   * error. The guard against that is the idempotent claim of the consumer tag in the router, NOT a
   * lock held across the call: taking the rebuild's own monitor here and then waiting inside it for a
   * channel that only a rebuild can produce is a deadlock, resolved only by the channel wait expiring.
   */
  public void resumeConsumer ()
    throws IOException {

    if ((!closed.get()) && isConsumerRequired()) {
      installConsumer();
    }
  }

  public void suspendConsumer ()
    throws IOException {

    if (!closed.get()) {
      uninstallConsumer();
    }
  }

  protected void markFullyBound (boolean bound) {

    fullyBound.set(bound);
  }

  public boolean isFullyBound () {

    return fullyBound.get();
  }

  public boolean isConnectionReplacementRequested () {

    return connectionReplacementRequested.get();
  }

  public int incrementChannelFailureCount () {

    return channelFailureCount.incrementAndGet();
  }

  public void resetChannelFailureCount () {

    channelFailureCount.set(0);
  }

  public void markClosed () {

    if (closed.compareAndSet(false, true)) {
      synchronized (channelRef) {
        discardChannel();
      }
    }
  }

  private void discardChannel () {

    synchronized (channelRef) {

      Channel channel;

      if ((channel = channelRef.getReference()) != null) {
        channelRef.set(null, channelRef.getStamp());
        try {
          channel.abort();
        } catch (Exception exception) {
          //  an unusable channel resisting abort changes nothing
        }
      }
    }
  }

  /*
   * The wait happens before any lock is taken. Waiting for a channel while holding the monitor a
   * rebuild needs in order to produce one is a stall with only one possible ending.
   */
  public void operate (ChannelOperation channelOperation)
    throws IOException {

    Channel channel = awaitChannel();

    synchronized (channel) {
      channelOperation.execute(channel);
    }
  }

  /*
   * Waits for the manager to produce a usable channel rather than dereferencing whatever happens to be
   * there, which was previously a null pointer exception on any path that ran before the first
   * successful build, or during a rebuild.
   */
  private Channel awaitChannel ()
    throws IOException {

    long stopTime = System.currentTimeMillis() + CHANNEL_WAIT_MILLISECONDS;
    Channel channel;

    while (((channel = channelRef.getReference()) == null) || (!channel.isOpen())) {
      if (closed.get() || connectionManager.isClosed()) {

        throw new FormattedIOException("The message router has been closed");
      }
      if (System.currentTimeMillis() >= stopTime) {

        throw new FormattedIOException("No channel became available within %d milliseconds", CHANNEL_WAIT_MILLISECONDS);
      }

      try {
        TimeUnit.MILLISECONDS.sleep(50);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();

        throw new FormattedIOException(interruptedException);
      }
    }

    return channel;
  }

  /*
   * Published with mandatory false. An unroutable message is meant to be dropped here - a shout has no
   * guaranteed audience, and a whisper or a response aimed at an instance that has gone away has
   * nowhere to be delivered, with in/out callers failing on their own response timeout. Asking the
   * broker to return messages nobody listens for is the worst of both.
   */
  public void send (String routingKey, String exchangeName, AMQP.BasicProperties properties, byte[] body)
    throws IOException, TimeoutException {

    if (!closed.get()) {

      long stopTime = System.currentTimeMillis() + CHANNEL_WAIT_MILLISECONDS;
      boolean sent = false;

      do {

        Channel channel = awaitChannel();
        int generation = channelRef.getStamp();

        try {
          synchronized (channel) {
            channel.basicPublish(exchangeName, routingKey, false, false, properties, body);
          }
          sent = true;
        } catch (AlreadyClosedException alreadyClosedException) {
          if (System.currentTimeMillis() >= stopTime) {

            throw new FormattedIOException(alreadyClosedException, "Unable to publish within %d milliseconds", CHANNEL_WAIT_MILLISECONDS);
          }

          //  The manager is already rebuilding on the back of the same shutdown signal - wait for the
          //  replacement channel rather than racing it with a rebuild of our own.
          connectionManager.onChannelFailure(this, generation, asShutdownSignal(alreadyClosedException));
          try {
            TimeUnit.MILLISECONDS.sleep(50);
          } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();

            throw new FormattedIOException(interruptedException);
          }
        }
      } while ((!sent) && (!closed.get()));
    }
  }

  public long getTimestamp (AMQP.BasicProperties properties) {

    Date date;

    if ((date = properties.getTimestamp()) != null) {

      return date.getTime();
    }

    return Long.MAX_VALUE;
  }

  public boolean isHealthy () {

    if (closed.get()) {

      return false;
    }

    Channel channel;

    if (((channel = channelRef.getReference()) == null) || (!channel.isOpen())) {

      return false;
    }

    if (!fullyBound.get()) {

      return false;
    }

    return (!isConsumerRequired()) || isConsumerInstalled();
  }

  public String getDiagnostic () {

    Channel channel = channelRef.getReference();

    return "router[channel(" + (((channel != null) && channel.isOpen()) ? "open" : "closed") + ") bound(" + fullyBound.get() + ") consuming(" + isConsumerInstalled() + ")]";
  }

  protected static boolean isNotFound (ShutdownSignalException shutdownSignalException) {

    Object reason;

    return ((reason = shutdownSignalException.getReason()) instanceof AMQP.Channel.Close) && (((AMQP.Channel.Close)reason).getReplyCode() == 404);
  }

  protected static ShutdownSignalException asShutdownSignal (Throwable throwable) {

    Throwable cursor = throwable;

    while (cursor != null) {
      if (cursor instanceof ShutdownSignalException) {

        return (ShutdownSignalException)cursor;
      }
      cursor = cursor.getCause();
    }

    return null;
  }

  public void close ()
    throws IOException, TimeoutException {

    markClosed();
  }
}
