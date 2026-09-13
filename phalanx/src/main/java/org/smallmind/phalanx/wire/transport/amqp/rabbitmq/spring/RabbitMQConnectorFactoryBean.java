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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq.spring;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import org.smallmind.phalanx.wire.transport.amqp.rabbitmq.RabbitMQConnector;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring factory bean that builds a {@link RabbitMQConnector} from configured servers, credentials and
 * optional recovery tuning.
 */
public class RabbitMQConnectorFactoryBean implements FactoryBean<RabbitMQConnector>, InitializingBean {

  private RabbitMQConnector rabbitMQConnector;
  private RabbitMQServer[] servers;
  private String username;
  private String password;
  private Double retryMultiplier;
  private Double retryJitterRatio;
  private Integer initialRetryMilliseconds;
  private Integer maximumRetryMilliseconds;
  private Integer channelFailureEscalationCount;
  private int heartbeatSeconds;

  /**
   * Sets the broker endpoints used to establish connections.
   *
   * @param servers array of broker endpoints.
   */
  public void setServers (RabbitMQServer[] servers) {

    this.servers = servers;
  }

  /**
   * Sets the username for broker authentication.
   *
   * @param username broker authentication username.
   */
  public void setUsername (String username) {

    this.username = username;
  }

  /**
   * Sets the password for broker authentication.
   *
   * @param password broker authentication password.
   */
  public void setPassword (String password) {

    this.password = password;
  }

  /**
   * Sets the heartbeat interval requested of the broker, which also bounds how quickly a dead
   * connection is noticed.
   *
   * @param heartbeatSeconds heartbeat interval in seconds.
   */
  public void setHeartbeatSeconds (int heartbeatSeconds) {

    this.heartbeatSeconds = heartbeatSeconds;
  }

  //  All optional, and all defaulted in RabbitMQConnector. Existing wiring needs no change.

  /**
   * Optionally overrides the delay before the first recovery attempt.
   *
   * @param initialRetryMilliseconds initial retry delay in milliseconds, or null for the default.
   */
  public void setInitialRetryMilliseconds (Integer initialRetryMilliseconds) {

    this.initialRetryMilliseconds = initialRetryMilliseconds;
  }

  /**
   * Optionally overrides the ceiling on the recovery delay.
   *
   * @param maximumRetryMilliseconds maximum retry delay in milliseconds, or null for the default.
   */
  public void setMaximumRetryMilliseconds (Integer maximumRetryMilliseconds) {

    this.maximumRetryMilliseconds = maximumRetryMilliseconds;
  }

  /**
   * Optionally overrides the backoff growth factor.
   *
   * @param retryMultiplier backoff multiplier, or null for the default.
   */
  public void setRetryMultiplier (Double retryMultiplier) {

    this.retryMultiplier = retryMultiplier;
  }

  /**
   * Optionally overrides the proportion of the delay applied as random jitter.
   *
   * @param retryJitterRatio jitter ratio, or null for the default.
   */
  public void setRetryJitterRatio (Double retryJitterRatio) {

    this.retryJitterRatio = retryJitterRatio;
  }

  /**
   * Optionally overrides how many consecutive channel rebuild failures are tolerated before the
   * connection is replaced.
   *
   * @param channelFailureEscalationCount failure count, or null for the default.
   */
  public void setChannelFailureEscalationCount (Integer channelFailureEscalationCount) {

    this.channelFailureEscalationCount = channelFailureEscalationCount;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link RabbitMQConnector} class.
   */
  @Override
  public Class<?> getObjectType () {

    return RabbitMQConnector.class;
  }

  /**
   * Returns whether the produced object is a singleton.
   *
   * @return true, because the connector is shared.
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the constructed connector.
   *
   * @return configured connector instance.
   */
  @Override
  public RabbitMQConnector getObject () {

    return rabbitMQConnector;
  }

  /**
   * Builds the connection factory and address list, then creates the connector and applies any
   * recovery tuning that was configured.
   */
  @Override
  public void afterPropertiesSet () {

    ConnectionFactory connectionFactory;
    Address[] addresses;
    int addressIndex = 0;

    addresses = new Address[servers.length];
    for (RabbitMQServer server : servers) {
      addresses[addressIndex++] = new Address(server.getHost(), server.getPort());
    }

    connectionFactory = new ConnectionFactory();
    //  RabbitMQConnectionManager owns recovery. Two recovery mechanisms racing on the same broker-side
    //  drop is how a connection ends up autorecovered behind the manager's back, with its consumers
    //  duplicated on queues the manager has already rebuilt elsewhere.
    connectionFactory.setAutomaticRecoveryEnabled(false);
    connectionFactory.setTopologyRecoveryEnabled(false);
    connectionFactory.setRequestedHeartbeat(heartbeatSeconds);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);

    rabbitMQConnector = new RabbitMQConnector(connectionFactory, addresses);

    if (initialRetryMilliseconds != null) {
      rabbitMQConnector.setInitialRetryMilliseconds(initialRetryMilliseconds);
    }
    if (maximumRetryMilliseconds != null) {
      rabbitMQConnector.setMaximumRetryMilliseconds(maximumRetryMilliseconds);
    }
    if (retryMultiplier != null) {
      rabbitMQConnector.setRetryMultiplier(retryMultiplier);
    }
    if (retryJitterRatio != null) {
      rabbitMQConnector.setRetryJitterRatio(retryJitterRatio);
    }
    if (channelFailureEscalationCount != null) {
      rabbitMQConnector.setChannelFailureEscalationCount(channelFailureEscalationCount);
    }
  }
}
