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

    ReconnectTestRouter router = new ReconnectTestRouter(connector);

    router.initialize();
    try {

      //  Kill the channel out from under the router; the registered shutdown listener rebuilds it
      //  (re-declaring the exchange and re-binding the queue) on another thread. abort() forces the
      //  close and discards any close-time error, so it fits the IOException-only ChannelOperation.
      router.operate(Channel::abort);

      //  Publishing now must succeed: send() either finds the rebuilt channel or hits AlreadyClosed,
      //  rebuilds, and retries. Either way the message must land on the bound queue.
      router.send(ROUTING_KEY, router.getRequestExchangeName(), new AMQP.BasicProperties.Builder().build(), "after-reconnect".getBytes());

      Assert.assertEquals(awaitDelivery(), "after-reconnect", "the message published after the channel was killed was not delivered");
    } finally {
      router.close();
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

    public ReconnectTestRouter (RabbitMQConnector connector) {

      super(connector, "wire-test", new NameConfiguration(), null);
    }

    @Override
    public void bindQueues ()
      throws IOException {

      operate(channel -> {
        //  Durable (not transient): this broker rejects transient non-exclusive queues, matching what
        //  ClassicQueueContractor declares in production.
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.queueBind(QUEUE_NAME, getRequestExchangeName(), ROUTING_KEY);
      });
    }

    @Override
    public void installConsumer () {

    }
  }
}
