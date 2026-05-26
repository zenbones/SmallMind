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
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Symmetric counterpart to {@code ReflectingPathIntegrationTest}. Confirms that
 * {@code streamingPaths} configured in Spring propagates through
 * {@code OumuamuaConfiguration.isStreaming}, is read once by the channel constructor,
 * and surfaces as {@code Channel.isStreaming() == true} on channels under the
 * configured prefix. On a streaming channel served by a non-long-polling transport
 * (WebSocket), {@code OumuamuaSession.deliver} (line 442) bypasses the long-poll
 * deque and writes straight to the connection; the unit test
 * {@code OumuamuaSessionTest.testDeliverStreamingBypassesQueue} verifies that path
 * directly. The wire-level effect — "no queueing" — is not observable from a
 * standard cometd client, so this test asserts the configuration-and-flag plumbing
 * that drives it, plus a normal round-trip to confirm the streaming branch does not
 * regress basic delivery.
 */
@Test(groups = "integration")
public class StreamingPathIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String[] STREAMING_SPRING_RESOURCE_LOCATIONS = new String[] {
    "org/smallmind/bayeux/oumuamua/server/impl/logging.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-streaming.xml"
  };
  private static final String STREAMING_CHANNEL = "/streaming/echo";
  private static final String NON_STREAMING_CHANNEL = "/integration/non-streaming";
  private static final String PAYLOAD = "streamed";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;

  @Override
  protected String[] springResourceLocations () {

    return STREAMING_SPRING_RESOURCE_LOCATIONS;
  }

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

  private Server<OrthodoxValue> oumuamuaServer () {

    return applicationContext().getBean(Server.class);
  }

  @Test
  public void streamingPathsAreParsedFromSpringConfig ()
    throws Exception {

    Assert.assertTrue(oumuamuaServer().isStreaming(new DefaultRoute(STREAMING_CHANNEL)), "Server's isStreaming returned false for " + STREAMING_CHANNEL + "; streamingPaths config did not take effect");
    Assert.assertFalse(oumuamuaServer().isStreaming(new DefaultRoute(NON_STREAMING_CHANNEL)), "Server's isStreaming returned true for " + NON_STREAMING_CHANNEL + "; streamingPaths matched too broadly");
  }

  @Test
  public void channelCreatedUnderStreamingPathIsConstructedWithStreamingFlag ()
    throws Exception {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch streamingSubscribeLatch = new CountDownLatch(1);

      client.getChannel(STREAMING_CHANNEL).subscribe(
        (channel, message) -> {
        },
        ackMessage -> streamingSubscribeLatch.countDown());

      Assert.assertTrue(streamingSubscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive for streaming channel");
      Assert.assertNotNull(oumuamuaServer().findChannel(STREAMING_CHANNEL));
      Assert.assertTrue(oumuamuaServer().findChannel(STREAMING_CHANNEL).isStreaming(), "Channel under streamingPaths should have isStreaming() == true");

      CountDownLatch unrelatedSubscribeLatch = new CountDownLatch(1);

      client.getChannel(NON_STREAMING_CHANNEL).subscribe(
        (channel, message) -> {
        },
        ackMessage -> unrelatedSubscribeLatch.countDown());

      Assert.assertTrue(unrelatedSubscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive for non-streaming channel");
      Assert.assertNotNull(oumuamuaServer().findChannel(NON_STREAMING_CHANNEL));
      Assert.assertFalse(oumuamuaServer().findChannel(NON_STREAMING_CHANNEL).isStreaming(), "Channel outside streamingPaths should have isStreaming() == false");
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void streamingChannelStillRoundTripsBetweenTwoClients ()
    throws InterruptedException {

    BayeuxClient subscriber = constructBayeuxClient();
    BayeuxClient publisher = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch deliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> deliveredMessage = new AtomicReference<>();

      subscriber.getChannel(STREAMING_CHANNEL).subscribe(
        (channel, message) -> {
          if (!message.isPublishReply()) {
            deliveredMessage.set(message);
            deliveryLatch.countDown();
          }
        },
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      publisher.getChannel(STREAMING_CHANNEL).publish(PAYLOAD);

      Assert.assertTrue(deliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Streaming-channel publish did not deliver");
      Assert.assertEquals(deliveredMessage.get().getData(), PAYLOAD);
      Assert.assertEquals(deliveredMessage.get().getChannel(), STREAMING_CHANNEL);
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
