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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * End-to-end RPC over a {@code /service/*} channel handled by a
 * {@link EchoBayeuxService}. The service is registered on the running
 * {@link OumuamuaServer} bean fetched from the Spring context, then a
 * {@link BayeuxClient} publishes to the service channel and the test asserts
 * the response message — tagged {@code RESPONSE} server-side and addressed to
 * the calling session only — arrives on the client's listener for the same
 * channel.
 */
@Test(groups = "integration")
public class BayeuxServiceIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String ECHO_SERVICE_CHANNEL = "/service/echo";
  private static final String PAYLOAD = "ping-from-client";
  private static final long HANDSHAKE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long AWAIT_TIMEOUT_MILLISECONDS = 10_000L;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();
    registerEchoService();
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  private void registerEchoService ()
    throws IOException {

    Server<OrthodoxValue> server = applicationContext().getBean(Server.class);

    try {
      server.addService(new EchoBayeuxService(new DefaultRoute(ECHO_SERVICE_CHANNEL)));
    } catch (Exception exception) {
      throw new IOException("Failed to register EchoBayeuxService", exception);
    }
  }

  @Test
  public void publishToServiceChannelReturnsServiceResponseToCaller ()
    throws InterruptedException {

    BayeuxClient client = constructBayeuxClient();

    try {
      handshakeAndAwait(client, HANDSHAKE_TIMEOUT_MILLISECONDS);

      CountDownLatch responseLatch = new CountDownLatch(1);
      AtomicReference<Message> responseReference = new AtomicReference<>();

      ClientSessionChannel channel = client.getChannel(ECHO_SERVICE_CHANNEL);

      channel.addListener((ClientSessionChannel.MessageListener)(c, message) -> {
        if (message.getData() != null) {
          responseReference.set(message);
          responseLatch.countDown();
        }
      });

      channel.publish(PAYLOAD);

      Assert.assertTrue(responseLatch.await(AWAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Service response did not arrive within " + AWAIT_TIMEOUT_MILLISECONDS + "ms");

      Message response = responseReference.get();

      Assert.assertNotNull(response, "Captured service response was null");
      Assert.assertTrue(response.isSuccessful(), "Service response was not marked successful: " + response);
      Assert.assertEquals(response.getData(), PAYLOAD, "Service response data did not echo the request payload");
      Assert.assertEquals(response.getChannel(), ECHO_SERVICE_CHANNEL);
      Assert.assertEquals(response.getClientId(), client.getId(), "Service response was not addressed to the calling session");
    } finally {
      disconnectAndAwait(client, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }
}
