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

import java.lang.reflect.Proxy;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Topic;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies the inbound-message safety net of {@link ResponseListener#onMessage}: a response that is
 * larger than the decode buffer, or whose payload is not a decodable {@link org.smallmind.phalanx.wire.signal.ResultSignal},
 * must be caught and logged rather than propagated — otherwise a single malformed reply would kill the
 * JMS delivery thread and stall every pending caller.  The listener is built against an in-process
 * Artemis broker; the bad messages are hand-built dynamic-proxy {@link jakarta.jms.BytesMessage}s fed
 * straight to {@code onMessage}.
 */
@Test(groups = "integration")
public class ResponseListenerInboundIntegrationTest {

  private static final int MAXIMUM_MESSAGE_LENGTH = 64;

  private EmbeddedActiveMQ broker;
  private ConnectionManager connectionManager;
  private ResponseListener responseListener;

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

    //  ConnectionManager.startup and onMessage's Claxon instrumentation resolve the registry from the
    //  per-application context; establish one on this thread.
    new PerApplicationContext();

    ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://0");
    Topic responseTopic = new ActiveMQTopic("inbound.response.topic");

    connectionManager = new ConnectionManager(new FixedManagedObjectFactory(connectionFactory, responseTopic), new MessagePolicy(), new ReconnectionPolicy());
    responseListener = new ResponseListener(null, connectionManager, responseTopic, new JsonSignalCodec(), "inbound-caller", MAXIMUM_MESSAGE_LENGTH);
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (responseListener != null) {
      responseListener.close();
    }
    if (broker != null) {
      broker.stop();
    }
  }

  private Message bytesMessage (long bodyLength) {

    return (Message)Proxy.newProxyInstance(ResponseListenerInboundIntegrationTest.class.getClassLoader(), new Class[] {jakarta.jms.BytesMessage.class}, (proxy, method, args) -> switch (method.getName()) {
      case "getLongProperty" -> 0L;
      case "getJMSMessageID" -> "inbound-msg-id";
      case "getJMSCorrelationID" -> "inbound-correlation-id";
      case "getBodyLength" -> bodyLength;
      case "readBytes" -> (int)Math.min(bodyLength, ((byte[])args[0]).length);
      default -> null;
    });
  }

  @Test
  public void testOversizedMessageIsSwallowed () {

    //  A body larger than the decode buffer must be rejected (TransportException) and caught, never
    //  propagated out of the delivery thread.
    responseListener.onMessage(bytesMessage(MAXIMUM_MESSAGE_LENGTH + 1));

    Assert.assertTrue(true, "onMessage must not propagate an oversized-message failure");
  }

  @Test
  public void testUndecodableMessageIsSwallowed () {

    //  A within-bounds body whose bytes are not a decodable ResultSignal must be caught, never
    //  propagated.
    responseListener.onMessage(bytesMessage(16));

    Assert.assertTrue(true, "onMessage must not propagate an undecodable-payload failure");
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
}
