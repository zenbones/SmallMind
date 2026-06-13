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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.servlet.WebappContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration coverage for the request routing of {@link WsadlHttpHandler}. A real {@link TyrusGrizzlyServerContainer}
 * is built with WSADL support enabled so the handler is registered on the running server, then HTTP requests drive the
 * three branches of the handler: the {@code application.wsadl} descriptor path, delegation to a supplied static handler
 * for non-descriptor requests, and the 404 fallback when no static handler is configured.
 */
@Test(groups = "integration")
public class WsadlHttpHandlerTest {

  @ServerEndpoint("/echo")
  public static class EchoServerEndpoint {

    @OnMessage
    public String onMessage (String message) {

      return "echo:" + message;
    }
  }

  public static class DelegateHttpHandler extends HttpHandler {

    @Override
    public void service (Request request, Response response)
      throws Exception {

      response.setStatus(200);
      response.setContentType("text/plain");
      response.getWriter().write("delegated");
    }
  }

  private static int freePort ()
    throws Exception {

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }

  private static HttpResponse<String> get (int port, String path)
    throws Exception {

    return HttpClient.newHttpClient().send(
      HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
      HttpResponse.BodyHandlers.ofString());
  }

  public void testNonWsadlRequestDelegatesToStaticHandler ()
    throws Exception {

    int port = freePort();
    HttpServer httpServer = new HttpServer();
    NetworkListener networkListener = new NetworkListener("grizzly-wsadl-delegate", "127.0.0.1", port);
    WebappContext webappContext = new WebappContext("wsadl-delegate-test", "");

    httpServer.addListener(networkListener);

    // Supplying a static handler exercises the delegation branch for requests that are not for the WSADL descriptor.
    TyrusGrizzlyServerContainer container = new TyrusGrizzlyServerContainer(httpServer, networkListener, webappContext, null, true, new DelegateHttpHandler());

    httpServer.start();

    try {

      container.register(EchoServerEndpoint.class);
      container.doneDeployment();
      container.start();

      HttpResponse<String> wsadlResponse = get(port, "/application.wsadl");
      HttpResponse<String> otherResponse = get(port, "/something/else");

      Assert.assertEquals(wsadlResponse.statusCode(), 200);
      Assert.assertTrue(wsadlResponse.body().contains("application"), "WSADL document should be returned");

      Assert.assertEquals(otherResponse.statusCode(), 200);
      Assert.assertEquals(otherResponse.body(), "delegated");
    } finally {
      container.stop();
      httpServer.shutdownNow();
    }
  }

  public void testNonWsadlRequestWithoutStaticHandlerReturnsNotFound ()
    throws Exception {

    int port = freePort();
    HttpServer httpServer = new HttpServer();
    NetworkListener networkListener = new NetworkListener("grizzly-wsadl-404", "127.0.0.1", port);
    WebappContext webappContext = new WebappContext("wsadl-404-test", "");

    httpServer.addListener(networkListener);

    // A null static handler routes non-descriptor requests through the 404 fallback branch.
    TyrusGrizzlyServerContainer container = new TyrusGrizzlyServerContainer(httpServer, networkListener, webappContext, null, true, null);

    httpServer.start();

    try {

      container.register(EchoServerEndpoint.class);
      container.doneDeployment();
      container.start();

      HttpResponse<String> wsadlResponse = get(port, "/application.wsadl");
      HttpResponse<String> missingResponse = get(port, "/no/such/path");

      Assert.assertEquals(wsadlResponse.statusCode(), 200);
      Assert.assertTrue(wsadlResponse.body().contains("application"), "WSADL document should be returned");

      Assert.assertEquals(missingResponse.statusCode(), 404);
    } finally {
      container.stop();
      httpServer.shutdownNow();
    }
  }
}
