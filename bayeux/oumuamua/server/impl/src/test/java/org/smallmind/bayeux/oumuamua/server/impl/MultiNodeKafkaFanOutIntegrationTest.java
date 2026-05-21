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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.cometd.bayeux.Message;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.websocket.jakarta.WebSocketTransport;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Cross-node fan-out via {@code KafkaBackbone}. Loads a second
 * Oumuamua stack on port {@code 9018} with a different {@code nodeName} but the
 * same Kafka topic as node A, then publishes from a client connected to node A
 * and asserts that a subscriber connected to node B receives the payload via
 * the Kafka relay. This is the only path that exercises the producer/consumer
 * loop in {@code KafkaBackbone} end to end; a single-node test relies entirely
 * on the local channel tree and never observes the wire format the backbone
 * exchanges between peers.
 */
@Test(groups = "integration")
public class MultiNodeKafkaFanOutIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String[] NODE_B_SPRING_RESOURCE_LOCATIONS = new String[] {
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly-b.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-b.xml"
  };
  private static final String NODE_B_SERVER_URL = "http://localhost:9018/smallmind/cometd";
  private static final String FAN_OUT_CHANNEL = "/integration/fan-out";
  private static final String PAYLOAD = "across-the-backbone";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 15_000L;
  private static final long CROSS_NODE_DELIVERY_TIMEOUT_MILLISECONDS = 30_000L;
  private static final long CROSS_NODE_RETRY_INTERVAL_MILLISECONDS = 1_000L;

  private ClassPathXmlApplicationContext nodeBContext;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();
    nodeBContext = new ClassPathXmlApplicationContext(NODE_B_SPRING_RESOURCE_LOCATIONS);
  }

  @AfterClass
  public void stopNodeBStack ()
    throws Exception {

    if (nodeBContext != null) {
      nodeBContext.close();
    }

    super.afterClass();
  }

  /**
   * Builds an unconnected {@link BayeuxClient} pointed at node B on port
   * {@code 9018}. Mirrors {@link #constructBayeuxClient()} which targets node A.
   *
   * @return WebSocket-backed {@link BayeuxClient} for node B
   */
  private BayeuxClient constructNodeBBayeuxClient () {

    WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
    ClientTransport transport = new WebSocketTransport(null, null, webSocketContainer);

    return new BayeuxClient(NODE_B_SERVER_URL, transport);
  }

  @Test
  public void messagePublishedOnNodeAReachesSubscriberOnNodeB ()
    throws InterruptedException {

    BayeuxClient nodeASubscriber = constructBayeuxClient();
    BayeuxClient nodeBSubscriber = constructNodeBBayeuxClient();
    BayeuxClient nodeAPublisher = constructBayeuxClient();

    try {
      handshakeAndAwait(nodeASubscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(nodeBSubscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(nodeAPublisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch nodeASubscribeLatch = new CountDownLatch(1);
      CountDownLatch nodeBSubscribeLatch = new CountDownLatch(1);
      CountDownLatch nodeBDeliveryLatch = new CountDownLatch(1);
      CountDownLatch nodeADeliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> nodeBDeliveredMessage = new AtomicReference<>();

      nodeASubscriber.getChannel(FAN_OUT_CHANNEL).subscribe(
        (channel, message) -> nodeADeliveryLatch.countDown(),
        ackMessage -> nodeASubscribeLatch.countDown());

      nodeBSubscriber.getChannel(FAN_OUT_CHANNEL).subscribe(
        (channel, message) -> {
          nodeBDeliveredMessage.set(message);
          nodeBDeliveryLatch.countDown();
        },
        ackMessage -> nodeBSubscribeLatch.countDown());

      Assert.assertTrue(nodeASubscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Node A subscribe acknowledgement did not arrive");
      Assert.assertTrue(nodeBSubscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Node B subscribe acknowledgement did not arrive");

      // The Kafka consumer on node B uses AUTO_OFFSET_RESET=latest, so any record produced before
      // the consumer's first partition assignment is dropped on the floor. Retry the publish
      // every CROSS_NODE_RETRY_INTERVAL until either delivery succeeds or the outer timeout
      // expires; multiple publishes are safe because the delivery latch only counts down once.
      boolean nodeBDelivered = false;
      long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CROSS_NODE_DELIVERY_TIMEOUT_MILLISECONDS);

      while (System.nanoTime() < deadlineNanos) {
        nodeAPublisher.getChannel(FAN_OUT_CHANNEL).publish(PAYLOAD);
        if (nodeBDeliveryLatch.await(CROSS_NODE_RETRY_INTERVAL_MILLISECONDS, TimeUnit.MILLISECONDS)) {
          nodeBDelivered = true;
          break;
        }
      }

      Assert.assertTrue(nodeBDelivered, "Published message did not cross the backbone to node B within " + CROSS_NODE_DELIVERY_TIMEOUT_MILLISECONDS + "ms");
      Assert.assertTrue(nodeADeliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Published message did not reach the local subscriber on node A");

      Message message = nodeBDeliveredMessage.get();

      Assert.assertNotNull(message, "Captured message reference on node B was null");
      Assert.assertEquals(message.getData(), PAYLOAD);
      Assert.assertEquals(message.getChannel(), FAN_OUT_CHANNEL);
    } finally {
      disconnectAndAwait(nodeAPublisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(nodeBSubscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(nodeASubscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
