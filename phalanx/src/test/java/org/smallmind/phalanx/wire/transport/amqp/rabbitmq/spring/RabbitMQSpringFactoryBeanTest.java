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

import org.smallmind.phalanx.wire.transport.amqp.rabbitmq.RabbitMQConnector;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the RabbitMQ Spring assembly helpers as plain objects: {@link RabbitMQServerFactoryBean}'s
 * pattern/spread expansion (single host, explicit port, and {@code #}-placeholder fan-out) and
 * {@link RabbitMQConnectorFactoryBean}'s connector construction. No broker is contacted — the
 * connector only wraps a {@code ConnectionFactory} until {@code getConnection()} is called.
 */
@Test(groups = "unit")
public class RabbitMQSpringFactoryBeanTest {

  @Test
  public void testServerFactoryBeanSingleHostDefaultPort ()
    throws Exception {

    RabbitMQServerFactoryBean factoryBean = new RabbitMQServerFactoryBean();

    factoryBean.setServerPattern("broker.example.com");
    factoryBean.afterPropertiesSet();

    RabbitMQServer[] servers = factoryBean.getObject();

    Assert.assertEquals(factoryBean.getObjectType(), RabbitMQServer[].class);
    Assert.assertTrue(factoryBean.isSingleton());
    Assert.assertEquals(servers.length, 1);
    Assert.assertEquals(servers[0].getHost(), "broker.example.com");
    Assert.assertEquals(servers[0].getPort(), 5672);
  }

  @Test
  public void testServerFactoryBeanExplicitPort ()
    throws Exception {

    RabbitMQServerFactoryBean factoryBean = new RabbitMQServerFactoryBean();

    factoryBean.setServerPattern("broker.example.com:5673");
    factoryBean.afterPropertiesSet();

    RabbitMQServer[] servers = factoryBean.getObject();

    Assert.assertEquals(servers.length, 1);
    Assert.assertEquals(servers[0].getHost(), "broker.example.com");
    Assert.assertEquals(servers[0].getPort(), 5673);
  }

  @Test
  public void testServerFactoryBeanSpreadFanOut ()
    throws Exception {

    RabbitMQServerFactoryBean factoryBean = new RabbitMQServerFactoryBean();

    factoryBean.setServerPattern("broker-#.example.com:5672");
    factoryBean.setServerSpread("1..3");
    factoryBean.afterPropertiesSet();

    RabbitMQServer[] servers = factoryBean.getObject();

    Assert.assertEquals(servers.length, 3);
    Assert.assertEquals(servers[0].getHost(), "broker-1.example.com");
    Assert.assertEquals(servers[1].getHost(), "broker-2.example.com");
    Assert.assertEquals(servers[2].getHost(), "broker-3.example.com");
    Assert.assertEquals(servers[2].getPort(), 5672);
  }

  @Test
  public void testConnectorFactoryBeanBuildsConnector ()
    throws Exception {

    RabbitMQConnectorFactoryBean factoryBean = new RabbitMQConnectorFactoryBean();

    factoryBean.setServers(new RabbitMQServer[] {new RabbitMQServer("localhost", 5672)});
    factoryBean.setUsername("guest");
    factoryBean.setPassword("guest");
    factoryBean.setHeartbeatSeconds(5);
    factoryBean.afterPropertiesSet();

    Assert.assertEquals(factoryBean.getObjectType(), RabbitMQConnector.class);
    Assert.assertTrue(factoryBean.isSingleton());
    Assert.assertNotNull(factoryBean.getObject());
  }
}
