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

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.glassfish.tyrus.ext.extension.deflate.PerMessageDeflateExtension;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies that the server's {@code PerMessageDeflateExtension}, wired into the
 * {@code WebsocketConfiguration} bean in {@code oumuamua.xml}, is advertised
 * through the WebSocket handshake and negotiated when a client opts in.
 * Driving this through cometd's {@code BayeuxClient} is not sufficient because
 * its {@code WebSocketTransport} only inserts the {@code permessage-deflate}
 * request header without participating in the full Tyrus extension negotiation;
 * the test instead opens a raw {@link jakarta.websocket} session with a real
 * client-side {@link PerMessageDeflateExtension} attached to the
 * {@link ClientEndpointConfig} and asserts that the server echoes the
 * extension back in {@link Session#getNegotiatedExtensions()}.
 */
@Test(groups = "integration")
public class PerMessageDeflateIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String PERMESSAGE_DEFLATE_NAME = "permessage-deflate";
  private static final long CONNECT_TIMEOUT_MILLISECONDS = 5_000L;

  private static String websocketUrl (String serverUrl) {

    if (serverUrl.startsWith("http://")) {

      return "ws://" + serverUrl.substring("http://".length());
    } else if (serverUrl.startsWith("https://")) {

      return "wss://" + serverUrl.substring("https://".length());
    } else {

      throw new IllegalArgumentException("Unsupported scheme in server URL: " + serverUrl);
    }
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
  public void serverNegotiatesPerMessageDeflateWhenClientOffersIt ()
    throws Exception {

    WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
    ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
                                            .extensions(List.of(new PerMessageDeflateExtension()))
                                            .build();
    CountDownLatch openLatch = new CountDownLatch(1);
    AtomicReference<Session> sessionReference = new AtomicReference<>();
    Endpoint endpoint = new Endpoint() {

      @Override
      public void onOpen (Session session, EndpointConfig config) {

        sessionReference.set(session);
        openLatch.countDown();
      }
    };

    Session session = webSocketContainer.connectToServer(endpoint, endpointConfig, URI.create(websocketUrl(serverUrl())));

    try {
      Assert.assertTrue(openLatch.await(CONNECT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "WebSocket session did not open within " + CONNECT_TIMEOUT_MILLISECONDS + "ms");

      List<Extension> negotiated = sessionReference.get().getNegotiatedExtensions();
      boolean deflateNegotiated = false;

      for (Extension extension : negotiated) {
        if (PERMESSAGE_DEFLATE_NAME.equals(extension.getName())) {
          deflateNegotiated = true;
          break;
        }
      }

      Assert.assertTrue(deflateNegotiated, "Server did not negotiate permessage-deflate; negotiated extensions were: " + negotiated);
    } finally {
      session.close();
    }
  }
}
