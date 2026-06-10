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
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.transport.AbstractWireTransportContractTest;
import org.smallmind.phalanx.wire.transport.RequestTransport;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.testbench.docker.DockerApplication;
import org.testng.annotations.Test;

/**
 * Runs the wire transport contract against an in-process Apache Artemis (Jakarta Messaging 3.x)
 * broker. No external infrastructure is required: the embedded broker uses an in-VM acceptor, so
 * this integration test runs anywhere the Artemis test dependencies resolve.
 */
@Test(groups = "integration")
public class JmsTransportIntegrationTest extends AbstractWireTransportContractTest {

  private final JsonSignalCodec signalCodec = new JsonSignalCodec();
  private EmbeddedActiveMQ broker;
  private ConnectionFactory connectionFactory;
  private RoutingFactories routingFactories;

  public JmsTransportIntegrationTest () {

    super((DockerApplication[])null);
  }

  @Override
  protected void prepareInfrastructure ()
    throws Exception {

    ConfigurationImpl configuration = new ConfigurationImpl();

    configuration.setPersistenceEnabled(false);
    configuration.setSecurityEnabled(false);
    configuration.addAcceptorConfiguration("in-vm", "vm://0");

    broker = new EmbeddedActiveMQ();
    broker.setConfiguration(configuration);
    broker.start();

    connectionFactory = new ActiveMQConnectionFactory("vm://0");
    routingFactories = new RoutingFactories(
      new EmbeddedManagedObjectFactory(connectionFactory, new ActiveMQQueue("wire.talk.queue")),
      new EmbeddedManagedObjectFactory(connectionFactory, new ActiveMQTopic("wire.request.topic")),
      new EmbeddedManagedObjectFactory(connectionFactory, new ActiveMQTopic("wire.response.topic")));
  }

  @Override
  protected void teardownInfrastructure ()
    throws Exception {

    if (broker != null) {
      broker.stop();
    }
  }

  @Override
  protected ResponseTransport createResponseTransport ()
    throws Exception {

    return new JmsResponseTransport(routingFactories, new MessagePolicy(), new ReconnectionPolicy(), signalCodec, "default", 1, 1, 1 << 20);
  }

  @Override
  protected RequestTransport createRequestTransport ()
    throws Exception {

    return new JmsRequestTransport(routingFactories, new MessagePolicy(), new ReconnectionPolicy(), signalCodec, 1, 1, 1 << 20, 30L);
  }

  private static class EmbeddedManagedObjectFactory implements ManagedObjectFactory {

    private final ConnectionFactory connectionFactory;
    private final Destination destination;

    private EmbeddedManagedObjectFactory (ConnectionFactory connectionFactory, Destination destination) {

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
}
