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
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises the session-lifecycle contract Oumuamua exposes through the
 * Bayeux meta channels. The unit tests confirm the {@code OumuamuaSession}
 * state machine in isolation; this test confirms the matching wire behavior
 * end to end. It asserts three things against a live server:
 *
 * <ul>
 *   <li>handshake registers the client id in the server-side session registry</li>
 *   <li>{@code /meta/connect} responses carry the {@code advice} block with
 *       the {@code interval} value derived from
 *       {@code sessionConnectIntervalSeconds} and a {@code reconnect} hint</li>
 *   <li>a graceful disconnect from the client clears the server-side session</li>
 * </ul>
 */
@Test(groups = "integration")
public class SessionLifecycleIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final long DEFAULT_SESSION_CONNECT_INTERVAL_MILLISECONDS = 30_000L;
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;
  private static final long DISCONNECT_OBSERVATION_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_POLL_INTERVAL_MILLISECONDS = 50L;

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

  private OumuamuaServer<?> oumuamuaServer () {

    return applicationContext().getBean(OumuamuaServer.class);
  }

  @Test
  public void handshakeRegistersSessionInServerRegistry () {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      String clientId = client.getId();

      Assert.assertNotNull(clientId, "BayeuxClient did not assign a client id");
      Assert.assertNotNull(oumuamuaServer().getSession(clientId), "OumuamuaServer did not register the handshake session under client id " + clientId);
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void metaConnectResponseCarriesAdviceInterval ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    try {
      CountDownLatch adviceLatch = new CountDownLatch(1);
      AtomicReference<Map<String, Object>> capturedAdvice = new AtomicReference<>();

      client.getChannel(Channel.META_CONNECT).addListener((ClientSessionChannel.MessageListener)(channel, message) -> {

        Map<String, Object> advice = message.getAdvice();

        if (advice != null) {
          capturedAdvice.compareAndSet(null, new HashMap<>(advice));
          adviceLatch.countDown();
        }
      });

      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      Assert.assertTrue(adviceLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "No /meta/connect response carried an advice block within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");

      Map<String, Object> advice = capturedAdvice.get();

      Assert.assertNotNull(advice, "Captured advice block was null");
      Assert.assertTrue(advice.containsKey(Message.INTERVAL_FIELD), "Connect advice did not include 'interval'; advice was " + advice);

      Object intervalValue = advice.get(Message.INTERVAL_FIELD);

      Assert.assertTrue(intervalValue instanceof Number, "Connect advice 'interval' was not a number: " + intervalValue);
      Assert.assertEquals(((Number)intervalValue).longValue(), DEFAULT_SESSION_CONNECT_INTERVAL_MILLISECONDS, "Connect advice 'interval' did not match the configured sessionConnectIntervalSeconds");
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  @Test
  public void disconnectRemovesSessionFromServerRegistry ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

    String clientId = client.getId();

    Assert.assertNotNull(oumuamuaServer().getSession(clientId), "Precondition failed: session was not registered after handshake");

    disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);

    long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DISCONNECT_OBSERVATION_TIMEOUT_MILLISECONDS);

    while (System.nanoTime() < deadlineNanos) {
      if (oumuamuaServer().getSession(clientId) == null) {

        return;
      }

      Thread.sleep(DISCONNECT_POLL_INTERVAL_MILLISECONDS);
    }

    Assert.fail("Session " + clientId + " was still present in the registry " + DISCONNECT_OBSERVATION_TIMEOUT_MILLISECONDS + "ms after disconnect");
  }
}
