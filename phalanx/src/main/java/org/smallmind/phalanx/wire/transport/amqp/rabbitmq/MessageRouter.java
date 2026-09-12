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

/**
 * Base class for routing AMQP messages, owning one channel of the connection held by a
 * {@link RabbitMQConnectionManager}, declaring and binding that router's queues, installing consumers,
 * and publishing.
 *
 * <p>A router does not own a connection and does not decide when to reconnect. The manager owns both,
 * calls {@link #rebuild} to build this router's channel, and is told of failures through the channel's
 * shutdown listener. Every router of a transport therefore shares one connection, which is what allows
 * the per-instance queues to be declared exclusively.
 */
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

  /**
   * Creates a router and registers it with the manager, which will build its channel when started.
   *
   * @param connectionManager            owner of the connection this router's channel is taken from.
   * @param prefix                       prefix prepended to all exchange and queue names.
   * @param nameConfiguration            suffix configuration for exchange and queue names.
   * @param publisherConfirmationHandler handler for publisher confirms, or {@code null} to disable confirms.
   */
  public MessageRouter (RabbitMQConnectionManager connectionManager, String prefix, NameConfiguration nameConfiguration, PublisherConfirmationHandler publisherConfirmationHandler) {

    this.connectionManager = connectionManager;
    this.prefix = prefix;
    this.nameConfiguration = nameConfiguration;
    this.publisherConfirmationHandler = publisherConfirmationHandler;

    connectionManager.register(this);
  }

  /**
   * Declares and binds the AMQP queues required by this router, recording how many of them succeeded
   * through {@link #markFullyBound}. A queue the broker refuses should leave the router degraded
   * rather than abort the others.
   *
   * @throws IOException if the connection is unusable.
   */
  public abstract void bindQueues ()
    throws IOException;

  /**
   * Installs AMQP consumers on the queues this router has successfully bound. Must be idempotent -
   * a rebuild and a resume can both reach it, and reusing a consumer tag is a connection level error.
   *
   * @throws IOException if consumer registration fails.
   */
  public abstract void installConsumer ()
    throws IOException;

  /**
   * Cancels the consumers this router installed, and only those - cancelling a consumer tag the broker
   * has never seen is a channel error.
   *
   * @throws IOException if cancelling fails.
   */
  public abstract void uninstallConsumer ()
    throws IOException;

  /**
   * Returns whether consumers should be installed at all, which for a response router follows the
   * transport's lifecycle state. A rebuild must not silently resume a transport an operator paused.
   *
   * @return true if this router should be consuming.
   */
  public abstract boolean isConsumerRequired ();

  /**
   * Returns whether consumers are presently installed on every queue this router serves.
   *
   * @return true if consumption is fully established.
   */
  public abstract boolean isConsumerInstalled ();

  /**
   * Returns the fully qualified request exchange name (prefix + suffix).
   *
   * @return request exchange name.
   */
  public String getRequestExchangeName () {

    return prefix + "-" + nameConfiguration.getRequestExchange();
  }

  /**
   * Returns the fully qualified response exchange name (prefix + suffix).
   *
   * @return response exchange name.
   */
  public String getResponseExchangeName () {

    return prefix + "-" + nameConfiguration.getResponseExchange();
  }

  /**
   * Returns the fully qualified response queue name (prefix + suffix).
   *
   * @return response queue name.
   */
  public String getResponseQueueName () {

    return prefix + "-" + nameConfiguration.getResponseQueue();
  }

  /**
   * Returns the fully qualified shout queue name (prefix + suffix).
   *
   * @return shout queue name.
   */
  public String getShoutQueueName () {

    return prefix + "-" + nameConfiguration.getShoutQueue();
  }

  /**
   * Returns the fully qualified talk queue name (prefix + suffix).
   *
   * @return talk queue name.
   */
  public String getTalkQueueName () {

    return prefix + "-" + nameConfiguration.getTalkQueue();
  }

  /**
   * Returns the fully qualified whisper queue name (prefix + suffix).
   *
   * @return whisper queue name.
   */
  public String getWhisperQueueName () {

    return prefix + "-" + nameConfiguration.getWhisperQueue();
  }

  /**
   * Builds this router's channel on the connection the manager currently owns, declaring the exchanges,
   * binding the queues, and installing consumers when they are required.
   *
   * <p>Called only from the {@link RabbitMQConnectionManager}, which serializes rebuilds and owns every
   * retry decision. This method never schedules a retry itself, and never swallows a connection level
   * failure - the manager needs to see that one in order to replace the connection.
   *
   * @param connection the connection to take a channel from.
   * @param generation the manager's generation stamp for that connection, carried by the channel so
   *                   that failures arriving from a superseded generation can be discarded.
   * @throws IOException if the channel cannot be built.
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

  /**
   * Declares and binds a single queue on a channel of its own, so that one queue the broker will not
   * hand over cannot abort the declaration of the others - a refused declaration closes its channel.
   *
   * <p>A channel level refusal returns false, leaving the router degraded but serving whatever did
   * bind. A connection level failure is rethrown, because nothing on this connection is salvageable.
   * A 404 against an ephemeral queue additionally requests a connection replacement, since that is the
   * signature of a queue record whose process has stopped and only a new connection frees the name.
   *
   * @param queueName        the queue being declared, used for logging and for the 404 decision.
   * @param ephemeral        true for a per-instance queue, whose name a new connection would free.
   * @param channelOperation the declaration and binding to perform.
   * @return true if the queue was declared and bound.
   * @throws IOException if the failure was connection level, or no connection is available.
   */
  protected boolean declareAndBind (String queueName, boolean ephemeral, ChannelOperation channelOperation)
    throws IOException {

    Connection connection;

    if ((connection = connectionManager.getConnection()) == null) {
      throw new FormattedIOException("No connection is available");
    }

    //  A channel of this declaration's own, closed however the attempt ends. A channel that will not
    //  close is one that was already in trouble, so its failure is classified alongside the
    //  declaration's rather than ignored - at worst that costs one more attempt at a declaration which
    //  is idempotent anyway.
    try (Channel declarationChannel = connection.createChannel()) {
      channelOperation.execute(declarationChannel);

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
  /**
   * Installs consumers if this router should be consuming, used when a paused transport is resumed.
   *
   * @throws IOException if consumer installation fails.
   */
  public void resumeConsumer ()
    throws IOException {

    if ((!closed.get()) && isConsumerRequired()) {
      installConsumer();
    }
  }

  /**
   * Cancels this router's consumers, used when a transport is paused. The queues themselves survive,
   * so messages arriving meanwhile are not lost.
   *
   * @throws IOException if cancelling fails.
   */
  public void suspendConsumer ()
    throws IOException {

    if (!closed.get()) {
      uninstallConsumer();
    }
  }

  /**
   * Records whether every queue this router needs was declared and bound.
   *
   * @param bound true if no queue was refused.
   */
  protected void markFullyBound (boolean bound) {

    fullyBound.set(bound);
  }

  /**
   * Returns whether every queue this router needs is currently declared and bound.
   *
   * @return true if the router is not degraded.
   */
  public boolean isFullyBound () {

    return fullyBound.get();
  }

  /**
   * Returns whether this router has met a condition only a new connection can clear, namely an
   * ephemeral queue whose process the broker reports as stopped.
   *
   * @return true if the manager should replace the connection.
   */
  public boolean isConnectionReplacementRequested () {

    return connectionReplacementRequested.get();
  }

  /**
   * Counts a consecutive channel level failure, used by the manager to decide when to stop retrying
   * the channel and replace the connection instead.
   *
   * @return the number of consecutive failures including this one.
   */
  public int incrementChannelFailureCount () {

    return channelFailureCount.incrementAndGet();
  }

  /**
   * Clears the consecutive channel failure count after a successful rebuild.
   */
  public void resetChannelFailureCount () {

    channelFailureCount.set(0);
  }

  /**
   * Marks this router closed and discards its channel. Called by the manager when the transport shuts
   * down; further operations fail rather than wait.
   */
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

  /**
   * Runs an operation against this router's channel, serialized on the channel itself so that two
   * threads never use one channel concurrently.
   *
   * <p>The wait for a channel happens before any lock is taken. Waiting for a channel while holding a
   * monitor that a rebuild needs in order to produce one is a stall with only one possible ending.
   *
   * @param channelOperation operation to execute against the current channel.
   * @throws IOException if no channel becomes available, or the operation fails.
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

  /**
   * Publishes a message, waiting for a usable channel and retrying across a rebuild if the channel is
   * closed underneath it.
   *
   * <p>Published with mandatory false. An unroutable message is meant to be dropped here - a shout has
   * no guaranteed audience, and a whisper or a response aimed at an instance that has gone away has
   * nowhere to be delivered, with in/out callers failing on their own response timeout. Asking the
   * broker to return messages that nobody listens for is the worst of both.
   *
   * @param routingKey   binding key for the target queue.
   * @param exchangeName exchange to publish to.
   * @param properties   message properties.
   * @param body         message payload.
   * @throws IOException      if no channel becomes available within the wait, or the publish fails.
   * @throws TimeoutException if establishing a channel times out.
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
   * Returns whether this router is carrying traffic: an open channel, every queue bound, and consumers
   * installed when they are required. A router deliberately not consuming, because its transport is
   * paused, is healthy.
   *
   * @return true if the router is able to carry traffic.
   */
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

  /**
   * Returns a short description of this router's channel, binding and consumer state.
   *
   * @return diagnostic description intended for an operator.
   */
  public String getDiagnostic () {

    Channel channel = channelRef.getReference();

    return "router[channel(" + (((channel != null) && channel.isOpen()) ? "open" : "closed") + ") bound(" + fullyBound.get() + ") consuming(" + isConsumerInstalled() + ")]";
  }

  /**
   * Returns whether a channel level failure carried reply code 404, which for an ephemeral queue is the
   * signature of a queue record whose process the broker has stopped.
   *
   * @param shutdownSignalException the failure to examine.
   * @return true if the broker answered NOT_FOUND.
   */
  protected static boolean isNotFound (ShutdownSignalException shutdownSignalException) {

    Object reason;

    return ((reason = shutdownSignalException.getReason()) instanceof AMQP.Channel.Close) && (((AMQP.Channel.Close)reason).getReplyCode() == 404);
  }

  /**
   * Unwraps a throwable down to the AMQP shutdown signal it carries, if any. The signal is what
   * distinguishes a channel level failure, which degrades one queue, from a connection level failure,
   * which condemns everything on the connection.
   *
   * @param throwable the throwable to unwrap.
   * @return the shutdown signal, or null if the throwable does not carry one.
   */
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

  /**
   * Marks this router closed. The connection itself belongs to the {@link RabbitMQConnectionManager},
   * which closes it when the transport shuts down.
   *
   * @throws IOException      never thrown; retained for compatibility with the previous contract.
   * @throws TimeoutException never thrown; retained for compatibility with the previous contract.
   */
  public void close ()
    throws IOException, TimeoutException {

    markClosed();
  }
}
