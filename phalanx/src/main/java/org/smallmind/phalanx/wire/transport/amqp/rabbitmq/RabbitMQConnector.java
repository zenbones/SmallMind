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
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

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

  public Connection getConnection ()
    throws IOException, TimeoutException {

    return connectionFactory.newConnection(addresses);
  }

  public int getInitialRetryMilliseconds () {

    return initialRetryMilliseconds;
  }

  public void setInitialRetryMilliseconds (int initialRetryMilliseconds) {

    this.initialRetryMilliseconds = initialRetryMilliseconds;
  }

  public int getMaximumRetryMilliseconds () {

    return maximumRetryMilliseconds;
  }

  public void setMaximumRetryMilliseconds (int maximumRetryMilliseconds) {

    this.maximumRetryMilliseconds = maximumRetryMilliseconds;
  }

  public double getRetryMultiplier () {

    return retryMultiplier;
  }

  public void setRetryMultiplier (double retryMultiplier) {

    this.retryMultiplier = retryMultiplier;
  }

  public double getRetryJitterRatio () {

    return retryJitterRatio;
  }

  public void setRetryJitterRatio (double retryJitterRatio) {

    this.retryJitterRatio = retryJitterRatio;
  }

  public int getChannelFailureEscalationCount () {

    return channelFailureEscalationCount;
  }

  public void setChannelFailureEscalationCount (int channelFailureEscalationCount) {

    this.channelFailureEscalationCount = channelFailureEscalationCount;
  }
}