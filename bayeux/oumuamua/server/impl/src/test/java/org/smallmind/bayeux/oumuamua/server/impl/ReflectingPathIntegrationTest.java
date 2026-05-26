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
 * Verifies the {@code reflectingPaths} delivery contract end to end. The {@code OumuamuaChannel}
 * delivery loop only fans a message back to the publishing session when the channel's reflecting
 * flag is set; the flag is decided at channel-creation time from
 * {@link Server#isReflecting(org.smallmind.bayeux.oumuamua.server.api.Route)}, which in turn
 * consults {@code OumuamuaConfiguration.reflectingPaths}. This test loads
 * {@code oumuamua-reflecting.xml} (which sets {@code reflectingPaths=/reflecting/**}) and asserts:
 *
 * <ul>
 *   <li>the server reports the configured path as reflecting and unrelated paths as not</li>
 *   <li>channel-creation reads that flag — the channel under {@code /reflecting/**} carries
 *       {@code isReflecting() == true}</li>
 *   <li>a client subscribed to its own reflecting channel receives the message it published</li>
 * </ul>
 *
 * <p>The negative case (publisher does not receive its own message on a non-reflecting channel)
 * is not asserted at the wire level here because the server still echoes the publisher's request
 * back to the caller in the publish response (controlled independently of {@code reflectingPaths}
 * by the {@code ext.oumuamua.echo} flag on the request, which defaults to true for cometd
 * compatibility — see {@code Meta.PUBLISH.getEchoFlag}). The unit test
 * {@code OumuamuaChannelTest.testDeliverSkipsSenderUnlessReflecting} covers the channel-level
 * skip; this integration test focuses on what is uniquely observable end to end.</p>
 */
@Test(groups = "integration")
public class ReflectingPathIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String[] REFLECTING_SPRING_RESOURCE_LOCATIONS = new String[] {
    "org/smallmind/bayeux/oumuamua/server/impl/logging.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-reflecting.xml"
  };
  private static final String REFLECTING_CHANNEL = "/reflecting/echo";
  private static final String NON_REFLECTING_CHANNEL = "/integration/non-reflecting";
  private static final String PAYLOAD = "hear-myself";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;

  @Override
  protected String[] springResourceLocations () {

    return REFLECTING_SPRING_RESOURCE_LOCATIONS;
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
  public void reflectingPathsAreParsedFromSpringConfig ()
    throws Exception {

    Assert.assertTrue(oumuamuaServer().isReflecting(new DefaultRoute(REFLECTING_CHANNEL)), "Server's isReflecting returned false for " + REFLECTING_CHANNEL + "; reflectingPaths config did not take effect");
    Assert.assertFalse(oumuamuaServer().isReflecting(new DefaultRoute(NON_REFLECTING_CHANNEL)), "Server's isReflecting returned true for " + NON_REFLECTING_CHANNEL + "; reflectingPaths matched too broadly");
  }

  @Test
  public void channelCreatedUnderReflectingPathIsConstructedWithReflectingFlag ()
    throws Exception {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);

      client.getChannel(REFLECTING_CHANNEL).subscribe(
        (channel, message) -> {
        },
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");
      Assert.assertNotNull(oumuamuaServer().findChannel(REFLECTING_CHANNEL), "Channel should exist after subscribe");
      Assert.assertTrue(oumuamuaServer().findChannel(REFLECTING_CHANNEL).isReflecting(), "Channel created under reflectingPaths should have isReflecting() == true");

      CountDownLatch unrelatedSubscribeLatch = new CountDownLatch(1);

      client.getChannel(NON_REFLECTING_CHANNEL).subscribe(
        (channel, message) -> {
        },
        ackMessage -> unrelatedSubscribeLatch.countDown());

      Assert.assertTrue(unrelatedSubscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive for unrelated channel");
      Assert.assertNotNull(oumuamuaServer().findChannel(NON_REFLECTING_CHANNEL));
      Assert.assertFalse(oumuamuaServer().findChannel(NON_REFLECTING_CHANNEL).isReflecting(), "Channel outside reflectingPaths should have isReflecting() == false");
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void publisherReceivesOwnMessageOnReflectingChannel ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch deliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> deliveredMessage = new AtomicReference<>();

      client.getChannel(REFLECTING_CHANNEL).subscribe(
        (channel, message) -> {
          if (!message.isPublishReply()) {
            deliveredMessage.set(message);
            deliveryLatch.countDown();
          }
        },
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      client.getChannel(REFLECTING_CHANNEL).publish(PAYLOAD);

      Assert.assertTrue(deliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Publisher did not receive its own message on a reflecting channel");

      Message message = deliveredMessage.get();

      Assert.assertEquals(message.getData(), PAYLOAD);
      Assert.assertEquals(message.getChannel(), REFLECTING_CHANNEL);
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
