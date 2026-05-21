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

import java.util.HashMap;
import java.util.Map;
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
 * Drives the {@code SecurityPolicy} denial path over the Bayeux meta channels.
 * Loads the server with a {@link RestrictedSecurityPolicy} wired into
 * {@code oumuamua-security.xml}, then asserts that subscribe and publish
 * attempts against a forbidden channel are rejected with the reason supplied
 * by the policy in the {@code error} field of the matching meta response.
 * Unrestricted channels continue to work, so the test also confirms that the
 * policy short-circuits exactly the rejected operations and nothing more.
 */
@Test(groups = "integration")
public class MetaChannelErrorIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String[] SECURITY_SPRING_RESOURCE_LOCATIONS = new String[] {
    "org/smallmind/bayeux/oumuamua/server/impl/logging.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-security.xml"
  };
  private static final String FORBIDDEN_CHANNEL = "/forbidden/secret";
  private static final String ALLOWED_CHANNEL = "/integration/allowed";
  private static final String CREATE_FORBIDDEN_CHANNEL = "/create-forbidden/fresh";
  private static final String UNAUTHORIZED_ERROR_PREFIX = "Unauthorized: ";
  private static final String HANDSHAKE_REJECTION_REASON = "Handshake denied by RestrictedSecurityPolicy";
  private static final String CREATE_REJECTION_REASON = "Channel creation denied by RestrictedSecurityPolicy";
  private static final String SUBSCRIBE_REJECTION_REASON = "Subscription denied by RestrictedSecurityPolicy";
  private static final String PUBLISH_REJECTION_REASON = "Publish denied by RestrictedSecurityPolicy";
  private static final String PAYLOAD = "should-not-fan-out";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;

  @Override
  protected String[] springResourceLocations () {

    return SECURITY_SPRING_RESOURCE_LOCATIONS;
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

  @Test
  public void subscribeToForbiddenChannelIsRejectedWithPolicyReason ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      AtomicReference<Message> subscribeAck = new AtomicReference<>();

      client.getChannel(FORBIDDEN_CHANNEL).subscribe(
        (channel, message) -> {
        },
        ackMessage -> {
          subscribeAck.set(ackMessage);
          subscribeLatch.countDown();
        });

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");

      Message ackMessage = subscribeAck.get();

      Assert.assertNotNull(ackMessage, "Captured subscribe acknowledgement was null");
      Assert.assertFalse(ackMessage.isSuccessful(), "Expected subscribe to forbidden channel to fail; got: " + ackMessage);
      Assert.assertEquals(ackMessage.get(Message.ERROR_FIELD), UNAUTHORIZED_ERROR_PREFIX + SUBSCRIBE_REJECTION_REASON, "Subscribe error field did not carry the policy reason; full ack: " + ackMessage);
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void publishToForbiddenChannelIsRejectedWithPolicyReason ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch publishLatch = new CountDownLatch(1);
      AtomicReference<Message> publishAck = new AtomicReference<>();

      client.getChannel(FORBIDDEN_CHANNEL).publish(PAYLOAD, ackMessage -> {
        publishAck.set(ackMessage);
        publishLatch.countDown();
      });

      Assert.assertTrue(publishLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Publish acknowledgement did not arrive within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");

      Message ackMessage = publishAck.get();

      Assert.assertNotNull(ackMessage, "Captured publish acknowledgement was null");
      Assert.assertFalse(ackMessage.isSuccessful(), "Expected publish to forbidden channel to fail; got: " + ackMessage);
      Assert.assertEquals(ackMessage.get(Message.ERROR_FIELD), UNAUTHORIZED_ERROR_PREFIX + PUBLISH_REJECTION_REASON, "Publish error field did not carry the policy reason; full ack: " + ackMessage);
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void allowedChannelStillRoundTripsUnderRestrictedPolicy ()
    throws InterruptedException {

    BayeuxClient subscriber = constructBayeuxClient();
    BayeuxClient publisher = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch deliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> subscribeAck = new AtomicReference<>();
      AtomicReference<Message> deliveredMessage = new AtomicReference<>();

      subscriber.getChannel(ALLOWED_CHANNEL).subscribe(
        (channel, message) -> {
          deliveredMessage.set(message);
          deliveryLatch.countDown();
        },
        ackMessage -> {
          subscribeAck.set(ackMessage);
          subscribeLatch.countDown();
        });

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive for allowed channel");
      Assert.assertTrue(subscribeAck.get().isSuccessful(), "Subscribe to allowed channel was rejected: " + subscribeAck.get());

      publisher.getChannel(ALLOWED_CHANNEL).publish(PAYLOAD);

      Assert.assertTrue(deliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Allowed channel did not deliver the message under RestrictedSecurityPolicy");
      Assert.assertEquals(deliveredMessage.get().getData(), PAYLOAD);
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void handshakeWithDenyExtFlagIsRejectedWithPolicyReason ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    try {
      Map<String, Object> handshakeTemplate = new HashMap<>();
      Map<String, Object> ext = new HashMap<>();

      ext.put("deny", true);
      handshakeTemplate.put("ext", ext);

      CountDownLatch handshakeLatch = new CountDownLatch(1);
      AtomicReference<Message> handshakeReply = new AtomicReference<>();

      client.handshake(handshakeTemplate, message -> {
        handshakeReply.set(message);
        handshakeLatch.countDown();
      });

      Assert.assertTrue(handshakeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Handshake reply did not arrive within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");

      Message reply = handshakeReply.get();

      Assert.assertNotNull(reply, "Captured handshake reply was null");
      Assert.assertFalse(reply.isSuccessful(), "Expected handshake with ext.deny=true to be rejected; got: " + reply);
      Assert.assertEquals(reply.get(Message.ERROR_FIELD), UNAUTHORIZED_ERROR_PREFIX + HANDSHAKE_REJECTION_REASON, "Handshake error field did not carry the policy reason; full reply: " + reply);
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void subscribeToReservedChannelPathIsRejectedAtCanCreate ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      AtomicReference<Message> subscribeAck = new AtomicReference<>();

      client.getChannel(CREATE_FORBIDDEN_CHANNEL).subscribe(
        (channel, message) -> {
        },
        ackMessage -> {
          subscribeAck.set(ackMessage);
          subscribeLatch.countDown();
        });

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");

      Message ackMessage = subscribeAck.get();

      Assert.assertNotNull(ackMessage, "Captured subscribe acknowledgement was null");
      Assert.assertFalse(ackMessage.isSuccessful(), "Expected subscribe to reserved-path channel to fail; got: " + ackMessage);
      Assert.assertEquals(ackMessage.get(Message.ERROR_FIELD), UNAUTHORIZED_ERROR_PREFIX + CREATE_REJECTION_REASON, "Subscribe error field did not carry the canCreate policy reason; full ack: " + ackMessage);
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
