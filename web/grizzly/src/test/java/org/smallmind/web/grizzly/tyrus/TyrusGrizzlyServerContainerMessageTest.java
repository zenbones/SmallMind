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
package org.smallmind.web.grizzly.tyrus;

import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerEndpoint;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.WebappContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration coverage for the WebSocket message lifecycle of {@link TyrusGrizzlyServerContainer}. A container is built
 * over a real Grizzly listener the same way {@link org.smallmind.web.grizzly.GrizzlyInitializingBean} builds it, an
 * annotated server endpoint is registered, the container is started, and a real Tyrus client connects, sends a message,
 * and receives the echoed reply, asserting the round trip across the container's {@code register}, {@code start},
 * {@code getPort}, and engine wiring.
 */
@Test(groups = "integration")
public class TyrusGrizzlyServerContainerMessageTest {

  @ServerEndpoint("/echo")
  public static class EchoServerEndpoint {

    @OnMessage
    public String onMessage (String message) {

      return "echo:" + message;
    }
  }

  @ClientEndpoint
  public static class RecordingClientEndpoint {

    private final AtomicReference<String> received = new AtomicReference<>();
    private final CountDownLatch latch = new CountDownLatch(1);

    @OnMessage
    public void onMessage (String message) {

      received.set(message);
      latch.countDown();
    }

    public String awaitMessage ()
      throws InterruptedException {

      latch.await(10, TimeUnit.SECONDS);

      return received.get();
    }
  }

  private static int freePort ()
    throws Exception {

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }

  public void testWebSocketEchoRoundTrip ()
    throws Exception {

    int port = freePort();
    HttpServer httpServer = new HttpServer();
    NetworkListener networkListener = new NetworkListener("grizzly-ws", "127.0.0.1", port);
    WebappContext webappContext = new WebappContext("ws-test", "");

    httpServer.addListener(networkListener);

    TyrusGrizzlyServerContainer container = new TyrusGrizzlyServerContainer(httpServer, networkListener, webappContext, null, false, null);

    httpServer.start();

    try {

      container.register(EchoServerEndpoint.class);
      container.doneDeployment();
      container.start();

      Assert.assertEquals(container.getPort(), port);

      WebSocketContainer clientContainer = ContainerProvider.getWebSocketContainer();
      RecordingClientEndpoint clientEndpoint = new RecordingClientEndpoint();

      try (Session session = clientContainer.connectToServer(clientEndpoint, URI.create("ws://127.0.0.1:" + port + "/echo"))) {
        session.getBasicRemote().sendText("ping");

        Assert.assertEquals(clientEndpoint.awaitMessage(), "echo:ping");
      }
    } finally {
      container.stop();
      httpServer.shutdownNow();
    }
  }
}
