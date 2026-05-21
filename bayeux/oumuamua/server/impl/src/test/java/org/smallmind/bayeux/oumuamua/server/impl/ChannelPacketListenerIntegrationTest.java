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
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Drives the channel-scoped {@link org.smallmind.bayeux.oumuamua.server.api.Channel.PacketListener}
 * veto path end to end. {@code OumuamuaChannelTest.testPacketListenerCanVeto} covers the listener
 * mechanics directly against a mocked channel; this test confirms the same veto logic actually
 * blocks delivery on the wire when the listener is attached to a real channel through a
 * {@link ChannelInitializer} registered on the running {@link Server}.
 *
 * <p>Two payloads are published from a separate client. The {@link VetoingChannelPacketListener}
 * vetoes deliveries whose data starts with {@code "VETO:"} and lets the others through. The
 * subscriber must receive the unvetoed payload and not the vetoed one, and the listener's
 * observation counters must reflect both invocations.</p>
 */
@Test(groups = "integration")
public class ChannelPacketListenerIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String VETO_CHANNEL = "/integration/veto";
  private static final String VETO_PREFIX = "VETO:";
  private static final String VETOED_PAYLOAD = VETO_PREFIX + "blocked";
  private static final String ALLOWED_PAYLOAD = "allowed";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;
  private static final long QUIET_PERIOD_MILLISECONDS = 1_000L;

  private VetoingChannelPacketListener listener;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();
    listener = new VetoingChannelPacketListener(VETO_PREFIX);
    attachListenerInitializer();
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  @SuppressWarnings("unchecked")
  private void attachListenerInitializer () {

    Server<OrthodoxValue> server = applicationContext().getBean(Server.class);
    ChannelInitializer<OrthodoxValue> initializer = channel -> {
      if (VETO_CHANNEL.equals(channel.getRoute().getPath())) {
        channel.addListener(listener);
      }
    };

    server.addInitializer(initializer);
  }

  @Test
  public void channelPacketListenerCanVetoDeliveriesOnTheWire ()
    throws InterruptedException {

    BayeuxClient subscriber = constructBayeuxClient();
    BayeuxClient publisher = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch allowedDeliveryLatch = new CountDownLatch(1);
      CountDownLatch vetoedDeliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> allowedMessage = new AtomicReference<>();
      AtomicReference<Message> vetoedMessage = new AtomicReference<>();

      subscriber.getChannel(VETO_CHANNEL).subscribe(
        (channel, message) -> {

          if (message.isPublishReply()) {

            return;
          }

          Object data = message.getData();

          if (ALLOWED_PAYLOAD.equals(data)) {
            allowedMessage.set(message);
            allowedDeliveryLatch.countDown();
          } else if (VETOED_PAYLOAD.equals(data)) {
            vetoedMessage.set(message);
            vetoedDeliveryLatch.countDown();
          }
        },
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      publisher.getChannel(VETO_CHANNEL).publish(VETOED_PAYLOAD);
      publisher.getChannel(VETO_CHANNEL).publish(ALLOWED_PAYLOAD);

      Assert.assertTrue(allowedDeliveryLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscriber did not receive the allowed payload; observedCount=" + listener.observedCount() + ", vetoedCount=" + listener.vetoedCount());
      Assert.assertEquals(allowedMessage.get().getData(), ALLOWED_PAYLOAD);

      Assert.assertFalse(vetoedDeliveryLatch.await(QUIET_PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscriber received the vetoed payload; the channel-scoped PacketListener did not block delivery. observedCount=" + listener.observedCount() + ", vetoedCount=" + listener.vetoedCount() + ", message=" + vetoedMessage.get());

      Assert.assertEquals(listener.vetoedCount(), 1, "Channel PacketListener should have vetoed exactly the prefixed payload; observedCount=" + listener.observedCount());
      Assert.assertTrue(listener.observedCount() >= 2, "Channel PacketListener should have seen at least both publishes; observedCount=" + listener.observedCount());
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
