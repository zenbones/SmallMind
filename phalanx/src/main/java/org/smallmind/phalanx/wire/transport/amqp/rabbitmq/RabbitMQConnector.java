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
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Creates connections to the configured brokers, and carries the retry tuning that
 * {@link RabbitMQConnectionManager} applies when recovering one.
 */
public class RabbitMQConnector {

  private static final int DEFAULT_INITIAL_RETRY_MILLISECONDS = 250;
  private static final int DEFAULT_MAXIMUM_RETRY_MILLISECONDS = 30000;
  private static final double DEFAULT_RETRY_MULTIPLIER = 2.0D;
  private static final double DEFAULT_RETRY_JITTER_RATIO = 0.2D;
  private static final int DEFAULT_CHANNEL_FAILURE_ESCALATION_COUNT = 3;

  private final ConnectionFactory connectionFactory;
  private final Address[] addresses;
  private double retryMultiplier = DEFAULT_RETRY_MULTIPLIER;
  private double retryJitterRatio = DEFAULT_RETRY_JITTER_RATIO;
  private int initialRetryMilliseconds = DEFAULT_INITIAL_RETRY_MILLISECONDS;
  private int maximumRetryMilliseconds = DEFAULT_MAXIMUM_RETRY_MILLISECONDS;
  private int channelFailureEscalationCount = DEFAULT_CHANNEL_FAILURE_ESCALATION_COUNT;

  /**
   * Wraps a connection factory and the broker endpoints to connect to, and disables the client
   * library's own recovery on that factory.
   *
   * @param connectionFactory factory used to open connections; its automatic and topology recovery are
   *                          turned off, since recovery belongs to {@link RabbitMQConnectionManager}
   *                          and two mechanisms racing on one drop leave duplicate consumers behind.
   * @param addresses         broker endpoints to connect to.
   */
  public RabbitMQConnector (ConnectionFactory connectionFactory, Address... addresses) {

    this.connectionFactory = connectionFactory;
    this.addresses = addresses;

    //  Recovery belongs to RabbitMQConnectionManager alone. Left on, the client library re-declares
    //  queues and re-registers consumers on a connection the manager has already replaced, which ends
    //  in two live connections consuming the same queues. Enforced here as well as in the factory bean
    //  so that a hand-built ConnectionFactory cannot quietly reintroduce it.
    connectionFactory.setAutomaticRecoveryEnabled(false);
    connectionFactory.setTopologyRecoveryEnabled(false);
  }

  /**
   * Opens a new connection to one of the configured brokers.
   *
   * @return a newly opened connection.
   * @throws IOException      if no broker could be reached.
   * @throws TimeoutException if the connection attempt timed out.
   */
  public Connection getConnection ()
    throws IOException, TimeoutException {

    return connectionFactory.newConnection(addresses);
  }

  /**
   * Returns the delay before the first recovery attempt.
   *
   * @return initial retry delay in milliseconds.
   */
  public int getInitialRetryMilliseconds () {

    return initialRetryMilliseconds;
  }

  /**
   * Sets the delay before the first recovery attempt.
   *
   * @param initialRetryMilliseconds initial retry delay in milliseconds.
   */
  public void setInitialRetryMilliseconds (int initialRetryMilliseconds) {

    this.initialRetryMilliseconds = initialRetryMilliseconds;
  }

  /**
   * Returns the ceiling on the recovery delay. The ceiling matters more than any attempt limit: a
   * broker that has lost majority cannot accept topology at all, and the only correct behaviour is to
   * keep asking patiently until it can.
   *
   * @return maximum retry delay in milliseconds.
   */
  public int getMaximumRetryMilliseconds () {

    return maximumRetryMilliseconds;
  }

  /**
   * Sets the ceiling on the recovery delay.
   *
   * @param maximumRetryMilliseconds maximum retry delay in milliseconds.
   */
  public void setMaximumRetryMilliseconds (int maximumRetryMilliseconds) {

    this.maximumRetryMilliseconds = maximumRetryMilliseconds;
  }

  /**
   * Returns the factor by which the recovery delay grows after each failed attempt.
   *
   * @return backoff multiplier.
   */
  public double getRetryMultiplier () {

    return retryMultiplier;
  }

  /**
   * Sets the factor by which the recovery delay grows after each failed attempt.
   *
   * @param retryMultiplier backoff multiplier.
   */
  public void setRetryMultiplier (double retryMultiplier) {

    this.retryMultiplier = retryMultiplier;
  }

  /**
   * Returns the proportion of the delay applied as random jitter, which keeps a fleet of pods from
   * retrying against a recovering broker in lockstep.
   *
   * @return jitter ratio, as a fraction of the delay.
   */
  public double getRetryJitterRatio () {

    return retryJitterRatio;
  }

  /**
   * Sets the proportion of the delay applied as random jitter.
   *
   * @param retryJitterRatio jitter ratio, as a fraction of the delay.
   */
  public void setRetryJitterRatio (double retryJitterRatio) {

    this.retryJitterRatio = retryJitterRatio;
  }

  /**
   * Returns how many consecutive channel rebuild failures are tolerated before the connection itself is
   * replaced. Replacing the connection is what frees this instance's exclusive queues, so it is the
   * escape from a queue the broker will not hand back.
   *
   * @return consecutive channel failures before escalation.
   */
  public int getChannelFailureEscalationCount () {

    return channelFailureEscalationCount;
  }

  /**
   * Sets how many consecutive channel rebuild failures are tolerated before the connection is replaced.
   *
   * @param channelFailureEscalationCount consecutive channel failures before escalation.
   */
  public void setChannelFailureEscalationCount (int channelFailureEscalationCount) {

    this.channelFailureEscalationCount = channelFailureEscalationCount;
  }
}