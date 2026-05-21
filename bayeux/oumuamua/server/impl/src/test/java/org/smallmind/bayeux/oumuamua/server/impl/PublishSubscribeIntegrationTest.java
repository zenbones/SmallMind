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
import org.cometd.bayeux.Message;
import org.cometd.client.BayeuxClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * End-to-end round trip across two {@link BayeuxClient} instances against a
 * single live Oumuamua server. A subscriber client subscribes to a channel,
 * waits for the {@code /meta/subscribe} acknowledgement, and asserts that a
 * payload published by a second client is delivered to its listener within
 * a bounded timeout. Exercises the full stack: Grizzly servlet wiring,
 * WebSocket transport, the meta-channel handlers, the channel tree, the
 * session registry, and {@code KafkaBackbone} fan-out.
 */
@Test(groups = "integration")
public class PublishSubscribeIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String ECHO_CHANNEL = "/integration/echo";
  private static final String PAYLOAD = "hello";
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 5_000L;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  @Test
  public void publishedMessageReachesRemoteSubscriber ()
    throws InterruptedException {

    BayeuxClient subscriber = constructBayeuxClient();
    BayeuxClient publisher = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber);
      handshakeAndAwait(publisher);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch deliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> subscribeAck = new AtomicReference<>();
      AtomicReference<Message> deliveredMessage = new AtomicReference<>();

      subscriber.getChannel(ECHO_CHANNEL).subscribe(
        (channel, message) -> {
          deliveredMessage.set(message);
          deliveryLatch.countDown();
        },
        ackMessage -> {
          subscribeAck.set(ackMessage);
          subscribeLatch.countDown();
        });

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");
      Assert.assertTrue(subscribeAck.get().isSuccessful(), "Subscribe was rejected: " + subscribeAck.get());

      publisher.getChannel(ECHO_CHANNEL).publish(PAYLOAD);

      Assert.assertTrue(deliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Published message did not reach the subscriber within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");

      Message message = deliveredMessage.get();

      Assert.assertNotNull(message, "Captured message reference was null");
      Assert.assertEquals(message.getData(), PAYLOAD);
      Assert.assertEquals(message.getChannel(), ECHO_CHANNEL);
    } finally {
      disconnectAndAwait(publisher);
      disconnectAndAwait(subscriber);
    }
  }
}
