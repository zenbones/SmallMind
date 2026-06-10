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
package org.smallmind.phalanx.wire.transport.jms;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.smallmind.phalanx.wire.TransportException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises {@link ConnectionManager}'s reconnection contract against an in-process Artemis broker:
 * a simulated provider failure ({@code onException}) must rebuild the connection and re-establish
 * registered consumers, and an exhausted {@link ReconnectionPolicy} against an unavailable factory
 * must give up cleanly rather than loop forever.
 */
@Test(groups = "integration")
public class JmsConnectionManagerIntegrationTest {

  private EmbeddedActiveMQ broker;
  private ConnectionFactory connectionFactory;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    ConfigurationImpl configuration = new ConfigurationImpl();

    configuration.setPersistenceEnabled(false);
    configuration.setSecurityEnabled(false);
    configuration.addAcceptorConfiguration("in-vm", "vm://0");

    broker = new EmbeddedActiveMQ();
    broker.setConfiguration(configuration);
    broker.start();

    connectionFactory = new ActiveMQConnectionFactory("vm://0");
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (broker != null) {
      broker.stop();
    }
  }

  @Test
  public void testReconnectionRebuildsSessionsAndConsumers ()
    throws Exception {

    ConnectionManager connectionManager = new ConnectionManager(new FixedManagedObjectFactory(connectionFactory, new ActiveMQTopic("recon.topic")), new MessagePolicy(), new ReconnectionPolicy());

    try {

      RecordingEmployer employer = new RecordingEmployer(new ActiveMQTopic("recon.topic"));

      connectionManager.createConsumer(employer);

      Session before = connectionManager.getSession(employer);

      Assert.assertNotNull(before);

      connectionManager.onException(new JMSException("simulated provider failure"));

      Session after = connectionManager.getSession(employer);

      Assert.assertNotNull(after);
      Assert.assertNotSame(after, before);
    } finally {
      connectionManager.close();
    }
  }

  @Test
  public void testReconnectionGivesUpAfterExhaustingAttempts ()
    throws Exception {

    FailingManagedObjectFactory factory = new FailingManagedObjectFactory(connectionFactory, new ActiveMQTopic("recon.dead.topic"));
    ReconnectionPolicy reconnectionPolicy = new ReconnectionPolicy();

    reconnectionPolicy.setReconnectionAttempts(2);
    reconnectionPolicy.setReconnectionDelayMilliseconds(50);

    ConnectionManager connectionManager = new ConnectionManager(factory, new MessagePolicy(), reconnectionPolicy);

    factory.startFailing();
    connectionManager.onException(new JMSException("permanent provider failure"));

    //  Reaching here means the bounded reconnection loop terminated rather than spinning forever.
    Assert.assertTrue(factory.getCreateAttempts() > 1);
  }

  private static class FixedManagedObjectFactory implements ManagedObjectFactory {

    private final ConnectionFactory connectionFactory;
    private final Destination destination;

    private FixedManagedObjectFactory (ConnectionFactory connectionFactory, Destination destination) {

      this.connectionFactory = connectionFactory;
      this.destination = destination;
    }

    @Override
    public Connection createConnection ()
      throws TransportException {

      try {
        return connectionFactory.createConnection();
      } catch (JMSException jmsException) {
        throw new TransportException(jmsException);
      }
    }

    @Override
    public Destination getDestination () {

      return destination;
    }
  }

  private static class FailingManagedObjectFactory implements ManagedObjectFactory {

    private final ConnectionFactory connectionFactory;
    private final Destination destination;
    private volatile boolean failing = false;
    private volatile int createAttempts = 0;

    private FailingManagedObjectFactory (ConnectionFactory connectionFactory, Destination destination) {

      this.connectionFactory = connectionFactory;
      this.destination = destination;
    }

    private void startFailing () {

      failing = true;
    }

    private int getCreateAttempts () {

      return createAttempts;
    }

    @Override
    public Connection createConnection ()
      throws TransportException {

      createAttempts++;

      if (failing) {
        throw new TransportException("broker is unavailable");
      }

      try {
        return connectionFactory.createConnection();
      } catch (JMSException jmsException) {
        throw new TransportException(jmsException);
      }
    }

    @Override
    public Destination getDestination () {

      return destination;
    }
  }

  private static class RecordingEmployer implements SessionEmployer, MessageListener {

    private final Destination destination;

    private RecordingEmployer (Destination destination) {

      this.destination = destination;
    }

    @Override
    public Destination getDestination () {

      return destination;
    }

    @Override
    public String getMessageSelector () {

      return null;
    }

    @Override
    public void onMessage (Message message) {

    }
  }
}
