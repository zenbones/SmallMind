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
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Drives the {@code /meta/unsubscribe} wire path. The companion subscribe handler is exercised
 * by the existing {@code PublishSubscribeIntegrationTest}, but the unsubscribe response — which
 * must echo the {@code subscription} field on success and stop subsequent publishes from
 * reaching the listener — is not asserted elsewhere. This test subscribes, confirms a publish
 * is delivered, unsubscribes, and asserts the unsubscribe acknowledgement is well-formed and
 * that a follow-up publish no longer triggers the listener.
 */
@Test(groups = "integration")
public class MetaUnsubscribeIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String UNSUBSCRIBE_CHANNEL = "/integration/unsubscribe";
  private static final String PAYLOAD = "before-unsubscribe";
  private static final String POST_PAYLOAD = "after-unsubscribe";
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
  public void unsubscribeResponseEchoesSubscriptionAndStopsFurtherDeliveries ()
    throws InterruptedException {

    BayeuxClient subscriber = constructBayeuxClient();
    BayeuxClient publisher = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch firstDeliveryLatch = new CountDownLatch(1);
      CountDownLatch unsubscribeLatch = new CountDownLatch(1);
      CountDownLatch postUnsubscribeDeliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> unsubscribeAck = new AtomicReference<>();
      AtomicReference<Boolean> postUnsubscribeReceived = new AtomicReference<>(Boolean.FALSE);
      ClientSessionChannel.MessageListener subscriberListener = (clientChannel, message) -> {

        if (message.isPublishReply()) {

          return;
        }

        if (firstDeliveryLatch.getCount() > 0) {
          firstDeliveryLatch.countDown();
        } else {
          postUnsubscribeReceived.set(Boolean.TRUE);
          postUnsubscribeDeliveryLatch.countDown();
        }
      };

      subscriber.getChannel(UNSUBSCRIBE_CHANNEL).subscribe(
        subscriberListener,
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      publisher.getChannel(UNSUBSCRIBE_CHANNEL).publish(PAYLOAD);
      Assert.assertTrue(firstDeliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscriber did not receive the message published before unsubscribe");

      subscriber.getChannel(UNSUBSCRIBE_CHANNEL).unsubscribe(
        subscriberListener,
        ackMessage -> {
          unsubscribeAck.set(ackMessage);
          unsubscribeLatch.countDown();
        });

      Assert.assertTrue(unsubscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Unsubscribe acknowledgement did not arrive");

      Message ack = unsubscribeAck.get();

      Assert.assertNotNull(ack, "Captured unsubscribe acknowledgement was null");
      Assert.assertEquals(ack.getChannel(), Channel.META_UNSUBSCRIBE, "Unsubscribe ack should be on " + Channel.META_UNSUBSCRIBE);
      Assert.assertTrue(ack.isSuccessful(), "Unsubscribe was rejected: " + ack);
      Assert.assertEquals(ack.get(Message.SUBSCRIPTION_FIELD), UNSUBSCRIBE_CHANNEL, "Unsubscribe ack should echo subscription path; full ack: " + ack);

      publisher.getChannel(UNSUBSCRIBE_CHANNEL).publish(POST_PAYLOAD);
      Assert.assertFalse(postUnsubscribeDeliveryLatch.await(QUIET_PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscriber received delivery after unsubscribe");
      Assert.assertFalse(postUnsubscribeReceived.get(), "Subscriber listener fired for post-unsubscribe publish");
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
