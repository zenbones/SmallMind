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
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.cometd.client.transport.ClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Round trip over the long-polling transport. The test config in
 * {@code oumuamua.xml} declares both a {@code WebsocketProtocol} and a
 * {@code ServletProtocol}; the {@code PublishSubscribeIntegrationTest}
 * exercises the WebSocket side, while this test drives the {@code AsyncOumuamuaServlet}
 * code path by handshaking with {@link JettyHttpClientTransport} so the
 * client and server negotiate {@code long-polling} as the connection type.
 */
@Test(groups = "integration")
public class LongPollingIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String ECHO_CHANNEL = "/integration/long-polling/echo";
  private static final String PAYLOAD = "polled";
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

  /**
   * Builds an unconnected {@link BayeuxClient} configured with the Jetty
   * HTTP transport. The transport name in the handshake will be
   * {@code long-polling}, matching the {@code ServletProtocol} wired in
   * {@code oumuamua.xml}.
   *
   * @return long-polling-backed {@link BayeuxClient}
   */
  private BayeuxClient constructLongPollingBayeuxClient () {

    Map<String, Object> options = new HashMap<>();
    ClientTransport transport = new JettyHttpClientTransport(serverUrl(), options, httpClient);

    return new BayeuxClient(serverUrl(), transport);
  }

  @Test
  public void publishedMessageReachesSubscriberOverLongPolling ()
    throws InterruptedException {

    BayeuxClient subscriber = constructLongPollingBayeuxClient();
    BayeuxClient publisher = constructLongPollingBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

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
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
