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
import java.util.concurrent.atomic.AtomicInteger;
import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.cometd.client.transport.ClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Demonstrates that the long-poll session queue is bounded by
 * {@code OumuamuaConfiguration.maxLongPollQueueSize} and that overflow drops the oldest
 * entries rather than blocking the producer. The test loads {@code oumuamua-overflow.xml}
 * (with {@code maxLongPollQueueSize=2}) and uses a {@link JettyHttpClientTransport} client
 * (long-polling, so {@code session.isLongPolling()} is true). A blocking listener on the
 * subscriber stalls connect-cycle progress for long enough that a publisher floods the
 * queue past its capacity; the assertion is that the subscriber receives fewer messages
 * than were published.
 *
 * <p>The unit test {@code OumuamuaSessionTest.testLongPollOverflowDropsOldest} already
 * verifies the queue eviction logic deterministically. This integration test confirms
 * the same behavior is reachable over the wire through the long-polling transport, the
 * meta-connect handler, and the configurable {@code maxLongPollQueueSize} property.</p>
 */
@Test(groups = "integration")
public class LongPollQueueOverflowIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String[] OVERFLOW_SPRING_RESOURCE_LOCATIONS = new String[] {
    "org/smallmind/bayeux/oumuamua/server/impl/logging.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-overflow.xml"
  };
  private static final String OVERFLOW_CHANNEL = "/integration/overflow";
  private static final int PUBLISHED_COUNT = 20;
  private static final long LISTENER_HOLD_MILLISECONDS = 1_500L;
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 15_000L;
  private static final long SUBSCRIBE_SETTLE_MILLISECONDS = 500L;

  private HttpClient httpClient;

  @Override
  protected String[] springResourceLocations () {

    return OVERFLOW_SPRING_RESOURCE_LOCATIONS;
  }

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

  private BayeuxClient constructLongPollingBayeuxClient () {

    Map<String, Object> options = new HashMap<>();
    ClientTransport transport = new JettyHttpClientTransport(serverUrl(), options, httpClient);

    return new BayeuxClient(serverUrl(), transport);
  }

  @Test
  public void floodingLongPollSessionPastMaxQueueSizeDropsMessages ()
    throws InterruptedException {

    BayeuxClient subscriber = constructLongPollingBayeuxClient();
    BayeuxClient publisher = constructLongPollingBayeuxClient();

    try {
      handshakeAndAwait(subscriber, HANDSHAKE_TIMEOUT_MILLISECONDS);
      handshakeAndAwait(publisher, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      AtomicInteger deliveredCount = new AtomicInteger();
      CountDownLatch firstDeliveryLatch = new CountDownLatch(1);

      subscriber.getChannel(OVERFLOW_CHANNEL).subscribe(
        (channel, message) -> {

          if (message.isPublishReply()) {

            return;
          }

          deliveredCount.incrementAndGet();
          if (firstDeliveryLatch.getCount() > 0) {
            firstDeliveryLatch.countDown();
            try {
              Thread.sleep(LISTENER_HOLD_MILLISECONDS);
            } catch (InterruptedException interruptedException) {
              Thread.currentThread().interrupt();
            }
          }
        },
        ackMessage -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      Thread.sleep(SUBSCRIBE_SETTLE_MILLISECONDS);

      for (int index = 0; index < PUBLISHED_COUNT; index++) {
        publisher.getChannel(OVERFLOW_CHANNEL).publish("flood-" + index);
      }

      Thread.sleep(LISTENER_HOLD_MILLISECONDS * 2);

      int received = deliveredCount.get();

      Assert.assertTrue(received > 0, "Subscriber received zero messages; expected at least the first delivery before the queue starts dropping");
      Assert.assertTrue(received < PUBLISHED_COUNT, "Subscriber received " + received + " of " + PUBLISHED_COUNT + " published messages; expected overflow to drop some, but none were dropped (maxLongPollQueueSize=2)");
    } finally {
      disconnectAndAwait(publisher, DISCONNECT_TIMEOUT_MILLISECONDS);
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
