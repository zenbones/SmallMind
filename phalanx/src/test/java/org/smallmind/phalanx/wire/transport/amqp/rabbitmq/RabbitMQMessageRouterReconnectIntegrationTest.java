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
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises the self-healing path in {@link MessageRouter} that the happy-path transport tests never
 * reach: when the underlying channel dies, the shutdown listener must rebuild the channel (re-declaring
 * exchanges and re-binding queues) and a subsequent {@link MessageRouter#send} must recover via its
 * close-and-retry loop rather than losing the message or wedging the router.  The channel is closed
 * out from under the router using only the router's own public {@link MessageRouter#operate} surface,
 * so no internal state is touched reflectively.
 */
@Test(groups = "integration")
public class RabbitMQMessageRouterReconnectIntegrationTest extends AbstractGroundwaterTest {

  private static final String QUEUE_NAME = "wire-test-reconnect-queue";
  private static final String ROUTING_KEY = "reconnect-key";

  private RabbitMQConnector connector;

  public RabbitMQMessageRouterReconnectIntegrationTest () {

    super(DockerApplication.RABBITMQ);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();
    super.beforeClass();

    awaitBroker();

    ConnectionFactory connectionFactory = new ConnectionFactory();

    connectionFactory.setUsername("guest");
    connectionFactory.setPassword("guest");
    connectionFactory.setVirtualHost("/");

    connector = new RabbitMQConnector(connectionFactory, new Address("localhost", 5672));
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  private void awaitBroker ()
    throws InterruptedException {

    ConnectionFactory probeFactory = new ConnectionFactory();

    probeFactory.setUsername("guest");
    probeFactory.setPassword("guest");
    probeFactory.setVirtualHost("/");

    for (int attempt = 0; attempt < 30; attempt++) {
      try (Connection connection = probeFactory.newConnection(new Address[] {new Address("localhost", 5672)})) {

        return;
      } catch (Exception exception) {
        Thread.sleep(1000);
      }
    }
  }

  @Test
  public void testSendRecoversAfterChannelClose ()
    throws Exception {

    RabbitMQConnectionManager connectionManager = new RabbitMQConnectionManager(connector, "reconnect-test");
    ReconnectTestRouter router = new ReconnectTestRouter(connectionManager);

    //  The manager owns the connection and drives every rebuild; registering happens in the router's
    //  constructor, so starting the manager is what builds this router's channel.
    connectionManager.start();
    try {

      //  Kill the channel out from under the router; the registered shutdown listener reports it and
      //  the manager rebuilds it on another thread.
      //
      //  The kill has to come from the broker. An abort() would be application initiated, and the
      //  manager deliberately ignores those - it aborts channels itself when replacing them, and must
      //  not treat its own tidying as a failure. A passive declare of a queue that does not exist is a
      //  404, which closes the channel the way a real channel level failure does.
      try {
        router.operate((channel) -> channel.queueDeclarePassive("wire-test-no-such-queue"));
        Assert.fail("the broker accepted a passive declare of a queue that should not exist");
      } catch (IOException ioException) {
        //  expected - this IS the failure the test is about
      }

      //  Publishing now must succeed: send() either finds the rebuilt channel or hits AlreadyClosed,
      //  rebuilds, and retries. Either way the message must land on the bound queue.
      router.send(ROUTING_KEY, router.getRequestExchangeName(), new AMQP.BasicProperties.Builder().build(), "after-reconnect".getBytes());

      Assert.assertEquals(awaitDelivery(), "after-reconnect", "the message published after the channel was killed was not delivered");
    } finally {
      connectionManager.close();
    }
  }

  private String awaitDelivery ()
    throws Exception {

    Connection connection = connector.getConnection();

    try {

      Channel channel = connection.createChannel();
      GetResponse getResponse = null;

      for (int attempt = 0; (attempt < 50) && (getResponse == null); attempt++) {
        if ((getResponse = channel.basicGet(QUEUE_NAME, true)) == null) {
          Thread.sleep(100);
        }
      }

      return (getResponse == null) ? null : new String(getResponse.getBody());
    } finally {
      connection.close();
    }
  }

  private static class ReconnectTestRouter extends MessageRouter {

    public ReconnectTestRouter (RabbitMQConnectionManager connectionManager) {

      super(connectionManager, "wire-test", new NameConfiguration(), null);
    }

    @Override
    public void bindQueues ()
      throws IOException {

      //  Durable and non-exclusive, unlike the per-instance queues in production - this test is about
      //  the channel coming back, and a shared queue that outlives the connection is the simpler
      //  subject for that. The broker rejects transient non-exclusive queues, hence durable.
      markFullyBound(declareAndBind(QUEUE_NAME, false, (channel) -> {

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.queueBind(QUEUE_NAME, getRequestExchangeName(), ROUTING_KEY);
      }));
    }

    @Override
    public boolean isConsumerRequired () {

      return false;
    }

    @Override
    public boolean isConsumerInstalled () {

      return false;
    }

    @Override
    public void uninstallConsumer () {

    }

    @Override
    public void installConsumer () {

    }
  }
}
