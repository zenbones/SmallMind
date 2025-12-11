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

public class RabbitMQConnectorFactoryBean implements FactoryBean<RabbitMQConnector>, InitializingBean {

  private RabbitMQConnector rabbitMQConnector;
  private RabbitMQServer[] servers;
  private String username;
  private String password;
  private int heartbeatSeconds;

  public void setServers (RabbitMQServer[] servers) {

    this.servers = servers;
  }

  public void setUsername (String username) {

    this.username = username;
  }

  public void setPassword (String password) {

    this.password = password;
  }

  public void setHeartbeatSeconds (int heartbeatSeconds) {

    this.heartbeatSeconds = heartbeatSeconds;
  }

  @Override
  public Class<?> getObjectType () {

    return RabbitMQConnector.class;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public RabbitMQConnector getObject () {

    return rabbitMQConnector;
  }

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
    connectionFactory.setAutomaticRecoveryEnabled(true);
    connectionFactory.setRequestedHeartbeat(heartbeatSeconds);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);

    rabbitMQConnector = new RabbitMQConnector(connectionFactory, addresses);
  }
}