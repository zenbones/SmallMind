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
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Confirms the three lifecycle {@code Server.Listener} subtypes fire end to end against a live
 * server. The {@link RecordingServerListener} implements {@code SessionListener},
 * {@code ChannelListener}, and {@code SubscriptionListener} simultaneously and is registered
 * with the {@link OumuamuaServer} bean before any client connects, so each handshake, subscribe,
 * unsubscribe, and disconnect should land in the matching recorder list. The packet listener
 * subtype is already exercised by {@code AckExtensionIntegrationTest}.
 */
@Test(groups = "integration")
public class ServerListenerIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String LISTENER_CHANNEL = "/integration/listener/observe";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;
  private static final long QUIET_PERIOD_MILLISECONDS = 1_000L;

  private RecordingServerListener recorder;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();
    recorder = new RecordingServerListener();
    registerRecorder();
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  private void registerRecorder () {

    Server<OrthodoxValue> server = applicationContext().getBean(Server.class);

    server.addListener(recorder);
  }

  @Test
  public void connectedSubscribedUnsubscribedDisconnectedCallbacksAllFire ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();
    String clientId;

    handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);
    clientId = client.getId();

    Assert.assertTrue(recorder.connectedSessions().contains(clientId), "SessionListener.onConnected did not record session " + clientId + "; recorded: " + recorder.connectedSessions());

    CountDownLatch subscribeLatch = new CountDownLatch(1);
    CountDownLatch unsubscribeLatch = new CountDownLatch(1);
    AtomicReference<Message> subscribeAck = new AtomicReference<>();
    ClientSessionChannel.MessageListener channelListener = (channel, message) -> {
    };

    client.getChannel(LISTENER_CHANNEL).subscribe(
      channelListener,
      ackMessage -> {
        subscribeAck.set(ackMessage);
        subscribeLatch.countDown();
      });

    Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");
    Assert.assertTrue(subscribeAck.get().isSuccessful(), "Subscribe was rejected: " + subscribeAck.get());

    String expectedPair = LISTENER_CHANNEL + "|" + clientId;

    Assert.assertTrue(recorder.createdChannels().contains(LISTENER_CHANNEL), "ChannelListener.onCreated did not record " + LISTENER_CHANNEL + "; recorded: " + recorder.createdChannels());
    Assert.assertTrue(recorder.subscribedPairs().contains(expectedPair), "SubscriptionListener.onSubscribed did not record " + expectedPair + "; recorded: " + recorder.subscribedPairs());

    client.getChannel(LISTENER_CHANNEL).unsubscribe(
      channelListener,
      ackMessage -> unsubscribeLatch.countDown());

    Assert.assertTrue(unsubscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Unsubscribe acknowledgement did not arrive");

    long unsubscribeDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(QUIET_PERIOD_MILLISECONDS);

    while ((System.nanoTime() < unsubscribeDeadline) && (!recorder.unsubscribedPairs().contains(expectedPair))) {
      Thread.sleep(50L);
    }

    Assert.assertTrue(recorder.unsubscribedPairs().contains(expectedPair), "SubscriptionListener.onUnsubscribed did not record " + expectedPair + "; recorded: " + recorder.unsubscribedPairs());

    disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);

    long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(QUIET_PERIOD_MILLISECONDS);

    while ((System.nanoTime() < deadlineNanos) && (!recorder.disconnectedSessions().contains(clientId))) {
      Thread.sleep(50L);
    }

    Assert.assertTrue(recorder.disconnectedSessions().contains(clientId), "SessionListener.onDisconnected did not record session " + clientId + "; recorded: " + recorder.disconnectedSessions());
  }
}
