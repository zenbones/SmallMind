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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownSignalException;
import org.smallmind.scribe.pen.LoggerManager;

/*
 * Owns the single connection shared by all of a transport's message routers, and supervises its
 * recovery. Two properties matter, and both were absent from the per-router recovery this replaces...
 *
 * 1) Every router of a transport declares the same per-instance queue names, and an exclusive queue
 *    belongs to a connection rather than to a channel, so the routers must share one connection for
 *    those queues to be declared exclusively at all.
 * 2) A recovery attempt that fails must schedule the next attempt. The previous implementation
 *    re-declared from the channel's own shutdown listener, so a failure to open a connection left no
 *    listener registered anywhere, and recovery simply ended.
 *
 * Recovery never runs on the client's shutdown dispatch thread. Listeners enqueue, and the work
 * happens on this manager's scheduled executor.
 */
public class RabbitMQConnectionManager {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicInteger generation = new AtomicInteger(0);
  private final AtomicInteger connectionFailureCount = new AtomicInteger(0);
  private final CopyOnWriteArrayList<MessageRouter> messageRouterList = new CopyOnWriteArrayList<>();
  private final ScheduledExecutorService retryExecutor;
  private final RabbitMQConnector connector;
  private final Object connectionLock = new Object();
  private final String transportName;
  private Connection connection;
  private String lastFailureDescription;

  public RabbitMQConnectionManager (RabbitMQConnector connector, String transportName) {

    this.connector = connector;
    this.transportName = transportName;

    retryExecutor = Executors.newSingleThreadScheduledExecutor((runnable) -> {

      Thread thread = new Thread(runnable, "RabbitMQConnectionManager[" + transportName + "]");

      thread.setDaemon(true);

      return thread;
    });
  }

  public void register (MessageRouter messageRouter) {

    messageRouterList.add(messageRouter);
  }

  /*
   * Fails fast. If the broker is unreachable at startup the transport should not come up pretending
   * otherwise - recovery is for connections that were once established.
   */
  public void start ()
    throws IOException, TimeoutException {

    synchronized (connectionLock) {
      if (!closed.get()) {
        replaceConnection();
      }
    }
  }

  public int getGeneration () {

    return generation.get();
  }

  public boolean isClosed () {

    return closed.get();
  }

  public Connection getConnection () {

    synchronized (connectionLock) {

      return connection;
    }
  }

  public void onChannelFailure (MessageRouter messageRouter, int failedGeneration, ShutdownSignalException shutdownSignalException) {

    if (!closed.get()) {
      if ((shutdownSignalException != null) && shutdownSignalException.isHardError()) {
        //  A connection level failure reported through a channel. Nothing is salvageable at channel scope.
        scheduleConnectionRebuild(failedGeneration, describe(shutdownSignalException));
      } else {
        scheduleChannelRebuild(messageRouter, failedGeneration, describe(shutdownSignalException));
      }
    }
  }

  public void onConnectionFailure (int failedGeneration, ShutdownSignalException shutdownSignalException) {

    if (!closed.get()) {
      scheduleConnectionRebuild(failedGeneration, describe(shutdownSignalException));
    }
  }

  private void scheduleChannelRebuild (MessageRouter messageRouter, int failedGeneration, String description) {

    scheduleChannelRebuild(messageRouter, failedGeneration, description, true);
  }

  /*
   * The escalatable flag separates two failures that look alike and are not. A channel that keeps
   * dying may well come back on a new connection, and replacing the connection is also what frees this
   * instance's exclusive queues - so that one escalates. A queue the broker refuses on its merits, a
   * talk queue already declared with different arguments being the obvious case, will be refused just
   * as firmly on a new connection, and replacing it would delete and re-create the exclusive queues
   * that ARE working, flapping their consumers every few seconds for as long as the operator problem
   * lasts. That one retries the channel indefinitely instead, at a delay that grows to the cap.
   *
   * The one binding failure that does warrant a new connection - an ephemeral queue whose process the
   * broker has stopped - is signalled separately and immediately by the router, and does not wait for
   * any escalation count.
   */
  private void scheduleChannelRebuild (MessageRouter messageRouter, int failedGeneration, String description, boolean escalatable) {

    if (failedGeneration == generation.get()) {

      int attempt = messageRouter.incrementChannelFailureCount();

      if (escalatable && (attempt >= connector.getChannelFailureEscalationCount())) {
        LoggerManager.getLogger(RabbitMQConnectionManager.class).warn("channel recovery for transport(%s) has failed %d times (%s) - replacing the connection", transportName, attempt, description);
        scheduleConnectionRebuild(failedGeneration, description);
      } else {
        logFailure(attempt, escalatable ? "channel" : "binding", description);
        schedule(attempt, () -> {

          synchronized (connectionLock) {
            if ((!closed.get()) && (failedGeneration == generation.get())) {
              try {
                messageRouter.rebuild(connection, failedGeneration);
                if (messageRouter.isConnectionReplacementRequested()) {
                  scheduleConnectionRebuild(failedGeneration, "a required queue reports a stopped process");
                } else if (!messageRouter.isFullyBound()) {
                  scheduleChannelRebuild(messageRouter, failedGeneration, "not every queue could be declared", false);
                } else {
                  messageRouter.resetChannelFailureCount();
                  LoggerManager.getLogger(RabbitMQConnectionManager.class).info("channel recovered for transport(%s)", transportName);
                }
              } catch (Exception exception) {
                onChannelFailure(messageRouter, failedGeneration, asShutdownSignal(exception));
              }
            }
          }
        });
      }
    }
  }

  private void scheduleConnectionRebuild (int failedGeneration, String description) {

    if (failedGeneration == generation.get()) {

      int attempt = connectionFailureCount.incrementAndGet();

      logFailure(attempt, "connection", description);
      schedule(attempt, () -> {

        synchronized (connectionLock) {
          if ((!closed.get()) && (failedGeneration == generation.get())) {
            try {
              replaceConnection();
              LoggerManager.getLogger(RabbitMQConnectionManager.class).info("connection recovered for transport(%s) at generation(%d)", transportName, generation.get());
            } catch (Exception exception) {
              //  Never let the chain end here. If this attempt failed, the next one is already scheduled.
              scheduleConnectionRebuild(failedGeneration, describeThrowable(exception));
            }
          }
        }
      });
    }
  }

  private void replaceConnection ()
    throws IOException, TimeoutException {

    synchronized (connectionLock) {

      Connection replacementConnection;
      int replacementGeneration;

      closeConnection();

      replacementConnection = connector.getConnection();
      replacementGeneration = generation.incrementAndGet();
      connection = replacementConnection;
      connectionFailureCount.set(0);
      lastFailureDescription = null;

      connection.addShutdownListener((cause) -> {

        if ((!closed.get()) && (!cause.isInitiatedByApplication())) {
          onConnectionFailure(replacementGeneration, cause);
        }
      });

      for (MessageRouter messageRouter : messageRouterList) {
        messageRouter.resetChannelFailureCount();
        try {
          messageRouter.rebuild(replacementConnection, replacementGeneration);
          if (!messageRouter.isFullyBound()) {
            //  Degraded rather than dead - whatever did bind keeps carrying traffic while the rest retries.
            scheduleChannelRebuild(messageRouter, replacementGeneration, "not every queue could be declared", false);
          }
        } catch (Exception exception) {

          ShutdownSignalException shutdownSignalException = asShutdownSignal(exception);

          if ((shutdownSignalException != null) && shutdownSignalException.isHardError()) {

            throw exception;
          }

          LoggerManager.getLogger(RabbitMQConnectionManager.class).error(exception);
          scheduleChannelRebuild(messageRouter, replacementGeneration, describeThrowable(exception));
        }
      }
    }
  }

  private void closeConnection () {

    synchronized (connectionLock) {
      if (connection != null) {
        //  Unconditional. A connection in automatic recovery reports itself closed while remaining very
        //  much alive, and skipping the close on that basis is how zombie connections with duplicate
        //  consumers appear. Automatic recovery is disabled besides, but the close stays unconditional.
        try {
          connection.abort((int)TimeUnit.SECONDS.toMillis(5));
        } catch (Exception exception) {
          LoggerManager.getLogger(RabbitMQConnectionManager.class).warn(exception);
        }
        connection = null;
      }
    }
  }

  private void schedule (int attempt, Runnable runnable) {

    if (!closed.get()) {
      try {
        retryExecutor.schedule(runnable, calculateDelayMilliseconds(attempt), TimeUnit.MILLISECONDS);
      } catch (Exception exception) {
        LoggerManager.getLogger(RabbitMQConnectionManager.class).error(exception);
      }
    }
  }

  /*
   * Exponential with jitter, and capped rather than bounded by attempt count. The cap is what matters
   * during a broker majority loss, when no client action can shorten the wait and the only correct
   * behavior is to keep asking patiently until the cluster can accept topology again.
   */
  private long calculateDelayMilliseconds (int attempt) {

    double delay = connector.getInitialRetryMilliseconds() * Math.pow(connector.getRetryMultiplier(), Math.max(0, attempt - 1));
    double capped = Math.min(delay, connector.getMaximumRetryMilliseconds());
    double jitter = capped * connector.getRetryJitterRatio() * ((ThreadLocalRandom.current().nextDouble() * 2) - 1);

    return Math.max(1, (long)(capped + jitter));
  }

  private void logFailure (int attempt, String scope, String description) {

    if (attempt <= 1) {
      LoggerManager.getLogger(RabbitMQConnectionManager.class).error("%s failure on transport(%s) - %s", scope, transportName, description);
    } else {
      LoggerManager.getLogger(RabbitMQConnectionManager.class).warn("%s recovery attempt(%d) on transport(%s) - %s", scope, attempt, transportName, description);
    }

    synchronized (connectionLock) {
      lastFailureDescription = description;
    }
  }

  private static ShutdownSignalException asShutdownSignal (Throwable throwable) {

    Throwable cursor = throwable;

    while (cursor != null) {
      if (cursor instanceof ShutdownSignalException) {

        return (ShutdownSignalException)cursor;
      }
      cursor = cursor.getCause();
    }

    return null;
  }

  /*
   * The broker's own words, verbatim. The conditions that matter here are all diagnosable on sight
   * from the reply text - '404 ... process is stopped by supervisor', '405 RESOURCE_LOCKED', and
   * '541 ... metadata store operation timed out' - but only if nobody paraphrases them.
   */
  private static String describe (ShutdownSignalException shutdownSignalException) {

    Object reason;

    if (shutdownSignalException == null) {

      return "cause unknown";
    }

    //  The reason is null for a socket that simply went away, which is exactly what a broker restart
    //  looks like - and 'null (connection level)' in an operator's log is worse than useless.
    reason = shutdownSignalException.getReason();

    return ((reason == null) ? ((shutdownSignalException.getMessage() == null) ? "connection lost" : shutdownSignalException.getMessage()) : reason.toString()) + ((shutdownSignalException.isHardError()) ? " (connection level)" : " (channel level)");
  }

  private static String describeThrowable (Throwable throwable) {

    ShutdownSignalException shutdownSignalException;

    if ((shutdownSignalException = asShutdownSignal(throwable)) != null) {

      return describe(shutdownSignalException);
    }

    return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
  }

  public boolean isHealthy () {

    if (closed.get()) {

      return false;
    }

    synchronized (connectionLock) {
      if ((connection == null) || (!connection.isOpen())) {

        return false;
      }
    }

    for (MessageRouter messageRouter : messageRouterList) {
      if (!messageRouter.isHealthy()) {

        return false;
      }
    }

    return true;
  }

  public String getDiagnostic () {

    StringBuilder diagnosticBuilder = new StringBuilder("transport(").append(transportName).append(") generation(").append(generation.get()).append(')');

    synchronized (connectionLock) {
      diagnosticBuilder.append(" connection(").append(((connection != null) && connection.isOpen()) ? "open" : "closed").append(')');
      if (lastFailureDescription != null) {
        diagnosticBuilder.append(" lastFailure(").append(lastFailureDescription).append(')');
      }
    }

    for (MessageRouter messageRouter : messageRouterList) {
      diagnosticBuilder.append(' ').append(messageRouter.getDiagnostic());
    }

    return diagnosticBuilder.toString();
  }

  public void close () {

    if (closed.compareAndSet(false, true)) {
      retryExecutor.shutdownNow();

      for (MessageRouter messageRouter : messageRouterList) {
        messageRouter.markClosed();
      }

      synchronized (connectionLock) {
        if (connection != null) {
          try {
            connection.close((int)TimeUnit.SECONDS.toMillis(5));
          } catch (Exception exception) {
            LoggerManager.getLogger(RabbitMQConnectionManager.class).warn(exception);
          }
          connection = null;
        }
      }
    }
  }
}
