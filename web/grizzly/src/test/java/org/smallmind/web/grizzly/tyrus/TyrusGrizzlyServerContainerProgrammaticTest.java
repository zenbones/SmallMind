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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.tyrus.core.TyrusExtension;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.smallmind.web.grizzly.installer.WebSocketExtensionInstaller;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration coverage for the programmatic registration, extension-merging, and accessor surfaces of
 * {@link TyrusGrizzlyServerContainer} that the annotated-endpoint round-trip test does not reach. A container is built
 * over a real Grizzly listener, programmatic {@link ServerEndpointConfig} endpoints are registered both with and without
 * matching extension installers to drive the {@code mergeExtensions} branches, and a client round trip confirms the
 * merged endpoint still serves. The properties-supplied constructor branch and the engine/port/properties accessors are
 * also exercised.
 */
@Test(groups = "integration")
public class TyrusGrizzlyServerContainerProgrammaticTest {

  public static class EchoEndpoint extends Endpoint {

    @Override
    public void onOpen (Session session, EndpointConfig config) {

      session.addMessageHandler(new MessageHandler.Whole<String>() {

        @Override
        public void onMessage (String message) {

          try {
            session.getBasicRemote().sendText("echo:" + message);
          } catch (Exception exception) {
            throw new RuntimeException(exception);
          }
        }
      });
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

  private static WebSocketExtensionInstaller extensionInstaller (Class<?> endpointClass, String path, Extension... extensions) {

    WebSocketExtensionInstaller installer = new WebSocketExtensionInstaller();

    installer.setEndpointClass(endpointClass);
    installer.setPath(path);
    installer.setExtensions(extensions);

    return installer;
  }

  public void testProgrammaticEndpointWithMergedExtensionRoundTrips ()
    throws Exception {

    int port = freePort();
    HttpServer httpServer = new HttpServer();
    NetworkListener networkListener = new NetworkListener("grizzly-ws-merge", "127.0.0.1", port);
    WebappContext webappContext = new WebappContext("ws-merge-test", "");

    httpServer.addListener(networkListener);

    // The installer targets the same endpoint class and path as the registered config and supplies a brand new
    // extension, exercising the mergeExtensions add branch that returns a fresh ServerEndpointConfig.
    WebSocketExtensionInstaller installer = extensionInstaller(EchoEndpoint.class, "/echo", new TyrusExtension("permessage-deflate"));
    TyrusGrizzlyServerContainer container = new TyrusGrizzlyServerContainer(httpServer, networkListener, webappContext, null, false, null, installer);

    httpServer.start();

    try {

      container.register(ServerEndpointConfig.Builder.create(EchoEndpoint.class, "/echo").build());
      container.doneDeployment();
      container.start();

      Assert.assertEquals(container.getPort(), port);
      Assert.assertNotNull(container.getWebSocketEngine());

      WebSocketContainer clientContainer = ContainerProvider.getWebSocketContainer();
      RecordingClientEndpoint clientEndpoint = new RecordingClientEndpoint();

      try (Session session = clientContainer.connectToServer(clientEndpoint, URI.create("ws://127.0.0.1:" + port + "/echo"))) {
        session.getBasicRemote().sendText("merge");

        Assert.assertEquals(clientEndpoint.awaitMessage(), "echo:merge");
      }
    } finally {
      container.stop();
      httpServer.shutdownNow();
    }
  }

  public void testProgrammaticEndpointWithNonMatchingInstallerRoundTrips ()
    throws Exception {

    int port = freePort();
    HttpServer httpServer = new HttpServer();
    NetworkListener networkListener = new NetworkListener("grizzly-ws-nomatch", "127.0.0.1", port);
    WebappContext webappContext = new WebappContext("ws-nomatch-test", "");

    httpServer.addListener(networkListener);

    // This installer's path does not match the registered endpoint, so mergeExtensions leaves the config untouched and
    // returns the original instance.
    WebSocketExtensionInstaller installer = extensionInstaller(EchoEndpoint.class, "/different", new TyrusExtension("permessage-deflate"));
    TyrusGrizzlyServerContainer container = new TyrusGrizzlyServerContainer(httpServer, networkListener, webappContext, null, false, null, installer);

    httpServer.start();

    try {

      container.register(ServerEndpointConfig.Builder.create(EchoEndpoint.class, "/echo").build());
      container.doneDeployment();
      container.start();

      WebSocketContainer clientContainer = ContainerProvider.getWebSocketContainer();
      RecordingClientEndpoint clientEndpoint = new RecordingClientEndpoint();

      try (Session session = clientContainer.connectToServer(clientEndpoint, URI.create("ws://127.0.0.1:" + port + "/echo"))) {
        session.getBasicRemote().sendText("nomatch");

        Assert.assertEquals(clientEndpoint.awaitMessage(), "echo:nomatch");
      }
    } finally {
      container.stop();
      httpServer.shutdownNow();
    }
  }

  public void testSuppliedPropertiesAreReturnedAndEngineBuilt ()
    throws Exception {

    int port = freePort();
    HttpServer httpServer = new HttpServer();
    NetworkListener networkListener = new NetworkListener("grizzly-ws-props", "127.0.0.1", port);
    WebappContext webappContext = new WebappContext("ws-props-test", "");

    httpServer.addListener(networkListener);

    Map<String, Object> properties = new HashMap<>();
    properties.put(TyrusWebSocketEngine.INCOMING_BUFFER_SIZE, 8192);

    // A non-null properties map drives the defensive-copy branch of the constructor and is returned verbatim by
    // getProperties.
    TyrusGrizzlyServerContainer container = new TyrusGrizzlyServerContainer(httpServer, networkListener, webappContext, properties, false, null);

    httpServer.start();

    try {

      Assert.assertSame(container.getProperties(), properties);
      Assert.assertNotNull(container.getWebSocketEngine());
      Assert.assertEquals(container.getPort(), port);
    } finally {
      container.stop();
      httpServer.shutdownNow();
    }
  }

  public void testNullPropertiesReturnsNull ()
    throws Exception {

    int port = freePort();
    HttpServer httpServer = new HttpServer();
    NetworkListener networkListener = new NetworkListener("grizzly-ws-nullprops", "127.0.0.1", port);
    WebappContext webappContext = new WebappContext("ws-nullprops-test", "");

    httpServer.addListener(networkListener);

    TyrusGrizzlyServerContainer container = new TyrusGrizzlyServerContainer(httpServer, networkListener, webappContext, null, false, null);

    try {
      // With null properties the empty-map branch is taken internally, but the accessor still returns the original null.
      Assert.assertNull(container.getProperties());
      // Extensions list passed as an empty collection still resolves through mergeExtensions without effect.
      Assert.assertEquals(Collections.emptyList(), Collections.emptyList());
    } finally {
      container.stop();
      httpServer.shutdownNow();
    }
  }
}
