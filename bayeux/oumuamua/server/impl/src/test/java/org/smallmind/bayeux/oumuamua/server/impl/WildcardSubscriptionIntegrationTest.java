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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.cometd.client.BayeuxClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Confirms the on-the-wire semantics of Bayeux wildcard subscriptions against
 * a live channel tree. Unit tests cover the matching logic on
 * {@code ChannelBranch} and {@code Route}; this test verifies the same rules
 * hold when a real client subscribes through {@code /meta/subscribe} and a
 * second client publishes to literal channels:
 *
 * <ul>
 *   <li>{@code /wild/*} matches single-segment children but not deeper paths</li>
 *   <li>{@code /deep/**} matches any number of trailing segments</li>
 * </ul>
 */
@Test(groups = "integration")
public class WildcardSubscriptionIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String SINGLE_WILDCARD_CHANNEL = "/wild/*";
  private static final String SINGLE_WILDCARD_FOO = "/wild/foo";
  private static final String SINGLE_WILDCARD_BAR = "/wild/bar";
  private static final String SINGLE_WILDCARD_DEEPER = "/wild/foo/deeper";
  private static final String DEEP_WILDCARD_CHANNEL = "/deep/**";
  private static final String DEEP_LEVEL_ONE = "/deep/alpha";
  private static final String DEEP_LEVEL_TWO = "/deep/alpha/beta";
  private static final String DEEP_LEVEL_THREE = "/deep/alpha/beta/gamma";
  private static final String PAYLOAD = "wildcard-payload";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;
  private static final long QUIET_PERIOD_MILLISECONDS = 1_000L;

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
  public void singleSegmentWildcardMatchesChildrenButNotDeeperPaths ()
    throws InterruptedException {

    BayeuxClient subscriber = constructBayeuxClient();
    BayeuxClient publisher = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch matchedDeliveryLatch = new CountDownLatch(2);
      Set<String> deliveredChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());

      subscriber.getChannel(SINGLE_WILDCARD_CHANNEL).subscribe(
        (channel, message) -> {
          deliveredChannels.add(message.getChannel());
          matchedDeliveryLatch.countDown();
        },
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      publisher.getChannel(SINGLE_WILDCARD_FOO).publish(PAYLOAD);
      publisher.getChannel(SINGLE_WILDCARD_BAR).publish(PAYLOAD);
      publisher.getChannel(SINGLE_WILDCARD_DEEPER).publish(PAYLOAD);

      Assert.assertTrue(matchedDeliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Single-wildcard subscriber did not receive both matching publishes; received only: " + deliveredChannels);

      Thread.sleep(QUIET_PERIOD_MILLISECONDS);

      Set<String> snapshot = new HashSet<>(deliveredChannels);

      Assert.assertTrue(snapshot.contains(SINGLE_WILDCARD_FOO), "/wild/* did not receive /wild/foo; received: " + snapshot);
      Assert.assertTrue(snapshot.contains(SINGLE_WILDCARD_BAR), "/wild/* did not receive /wild/bar; received: " + snapshot);
      Assert.assertFalse(snapshot.contains(SINGLE_WILDCARD_DEEPER), "/wild/* should not have received /wild/foo/deeper but did; received: " + snapshot);
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void deepWildcardMatchesAllDescendants ()
    throws InterruptedException {

    BayeuxClient subscriber = constructBayeuxClient();
    BayeuxClient publisher = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch deliveryLatch = new CountDownLatch(3);
      Set<String> deliveredChannels = Collections.newSetFromMap(new ConcurrentHashMap<>());

      subscriber.getChannel(DEEP_WILDCARD_CHANNEL).subscribe(
        (channel, message) -> {
          deliveredChannels.add(message.getChannel());
          deliveryLatch.countDown();
        },
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      publisher.getChannel(DEEP_LEVEL_ONE).publish(PAYLOAD);
      publisher.getChannel(DEEP_LEVEL_TWO).publish(PAYLOAD);
      publisher.getChannel(DEEP_LEVEL_THREE).publish(PAYLOAD);

      Assert.assertTrue(deliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "/deep/** did not receive all three publishes; received: " + deliveredChannels);
      Assert.assertTrue(deliveredChannels.contains(DEEP_LEVEL_ONE), "/deep/** did not receive " + DEEP_LEVEL_ONE);
      Assert.assertTrue(deliveredChannels.contains(DEEP_LEVEL_TWO), "/deep/** did not receive " + DEEP_LEVEL_TWO);
      Assert.assertTrue(deliveredChannels.contains(DEEP_LEVEL_THREE), "/deep/** did not receive " + DEEP_LEVEL_THREE);
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
