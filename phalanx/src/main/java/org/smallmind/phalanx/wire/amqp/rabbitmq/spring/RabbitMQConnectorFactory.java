/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.phalanx.wire.amqp.rabbitmq.spring;

import java.util.ArrayList;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.phalanx.wire.amqp.rabbitmq.RabbitMQConnector;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class RabbitMQConnectorFactory implements FactoryBean<RabbitMQConnector>, InitializingBean {

  /*
  rabbitmq.host.<#> (required)
  rabbitmq.port.<#> (defaults to 5672)
  */

  private RabbitMQConnector rabbitMQConnector;
  private String username;
  private String password;
  private int heartbeatSeconds;

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

    SpringPropertyAccessor springPropertyAccessor = SpringPropertyAccessorManager.getSpringPropertyAccessor();
    ConnectionFactory connectionFactory;
    Address[] addresses;
    ArrayList<RabbitMQServer> serverList = new ArrayList<>();
    int addressIndex = 0;

    for (String key : springPropertyAccessor.getKeySet()) {
      if (key.startsWith("rabbitmq.host.")) {

        int index = Integer.valueOf(key.substring("rabbitmq.host.".length()));

        while (serverList.size() < index + 1) {
          serverList.add(new RabbitMQServer());
        }

        serverList.get(index).setHost(springPropertyAccessor.asString(key));
      } else if (key.startsWith("rabbitmq.port.")) {

        int index = Integer.valueOf(key.substring("rabbitmq.host.".length()));

        while (serverList.size() < index + 1) {
          serverList.add(new RabbitMQServer());
        }

        serverList.get(index).setPort(springPropertyAccessor.asInt(key).get());
      }
    }

    addresses = new Address[serverList.size()];
    for (RabbitMQServer server : serverList) {
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