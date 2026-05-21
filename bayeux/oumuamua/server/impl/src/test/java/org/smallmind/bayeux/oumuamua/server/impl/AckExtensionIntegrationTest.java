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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.ext.AckExtension;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.cometd.client.transport.ClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises the {@code ack} Bayeux extension end to end. The server-side
 * {@code AckExtension} is wired in {@code oumuamua.xml} as a server listener;
 * this test attaches the matching cometd client-side {@code AckExtension} so
 * both sides agree to track at-least-once delivery, then asserts the on-the-wire
 * negotiation visible in the handshake reply and the running ack-id counter
 * stamped onto each {@code /meta/connect} response that carries a delivery.
 * The ack extension forces all messages through long polling, so the client
 * uses {@link JettyHttpClientTransport}.
 */
@Test(groups = "integration")
public class AckExtensionIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String ACK_CHANNEL = "/integration/ack";
  private static final String PAYLOAD_PREFIX = "ack-payload-";
  private static final int PAYLOAD_COUNT = 3;
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;

  private HttpClient httpClient;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    httpClient = new HttpClient();
    try {
      httpClient.start();
    } catch (Exception startException) {
      throw new IllegalStateException("Unable to start Jetty HttpClient", startException);
    }
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (httpClient != null) {
      httpClient.stop();
    }

    super.afterClass();
  }

  private BayeuxClient constructAckEnabledBayeuxClient () {

    Map<String, Object> options = new HashMap<>();
    ClientTransport transport = new JettyHttpClientTransport(serverUrl(), options, httpClient);
    BayeuxClient bayeuxClient = new BayeuxClient(serverUrl(), transport);

    bayeuxClient.addExtension(new AckExtension());

    return bayeuxClient;
  }

  @Test
  public void handshakeAcknowledgesAckExtensionAndConnectsStampAckIds ()
    throws InterruptedException {

    BayeuxClient subscriber = constructAckEnabledBayeuxClient();
    BayeuxClient publisher = constructAckEnabledBayeuxClient();

    try {
      AtomicReference<Message> subscriberHandshake = new AtomicReference<>();
      AtomicLong maxConnectAckId = new AtomicLong(-1L);

      subscriber.getChannel(Channel.META_CONNECT).addListener((ClientSessionChannel.MessageListener)(channel, message) -> {

        Map<String, Object> ext = message.getExt();

        if (ext != null) {

          Object ack = ext.get("ack");

          if (ack instanceof Number) {
            maxConnectAckId.accumulateAndGet(((Number)ack).longValue(), Math::max);
          }
        }
      });

      CountDownLatch handshakeLatch = new CountDownLatch(1);

      subscriber.handshake(new HashMap<>(), message -> {
        subscriberHandshake.set(message);
        handshakeLatch.countDown();
      });

      Assert.assertTrue(handshakeLatch.await(HANDSHAKE_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Handshake response did not arrive within " + HANDSHAKE_TIMEOUT_MILLISECONDS + "ms");
      Assert.assertTrue(subscriber.waitFor(HANDSHAKE_TIMEOUT_MILLISECONDS, BayeuxClient.State.CONNECTED), "Subscriber did not reach CONNECTED");

      Message handshakeMessage = subscriberHandshake.get();
      Map<String, Object> handshakeExt = handshakeMessage.getExt();

      Assert.assertNotNull(handshakeExt, "Handshake reply carried no ext block");
      Assert.assertEquals(handshakeExt.get("ack"), Boolean.TRUE, "Handshake reply did not confirm ack extension support");

      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch deliveryLatch = new CountDownLatch(PAYLOAD_COUNT);

      subscriber.getChannel(ACK_CHANNEL).subscribe(
        (channel, message) -> deliveryLatch.countDown(),
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      for (int index = 0; index < PAYLOAD_COUNT; index++) {
        publisher.getChannel(ACK_CHANNEL).publish(PAYLOAD_PREFIX + index);
      }

      Assert.assertTrue(deliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Not all published messages reached the subscriber");
      Assert.assertTrue(maxConnectAckId.get() >= 0L, "No /meta/connect response carried an ext.ack id; expected the server to stamp connects when they piggy-back deliveries");
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
