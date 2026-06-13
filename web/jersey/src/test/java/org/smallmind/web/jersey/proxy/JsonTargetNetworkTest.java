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
package org.smallmind.web.jersey.proxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.WebApplicationException;
import org.smallmind.scribe.pen.AbstractAppender;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration test that drives {@link JsonTarget}'s network methods against an in-process JDK {@link HttpServer} bound
 * to an ephemeral port (no external services). It exercises the get/put/post/patch/delete round trips, the empty-body
 * {@code 2xx} =&gt; {@code null} path, the non-{@code 2xx} =&gt; {@link WebApplicationException} path, header and query
 * application, the request/response debug collectors driven through the scribe logging pipeline, and the context-prefix
 * path composition supplied by {@link JsonTargetFactory}.
 *
 * <p>The round-trip assertions deserialize the JSON response body into the {@link Echo} POJO via
 * {@code JsonCodec.read(byte[], Class)}, confirming the full request-through-response path returns a typed object.
 */
@Test(groups = "integration")
public class JsonTargetNetworkTest {

  private final AtomicReference<RecordedRequest> lastRequestRef = new AtomicReference<>();
  private HttpServer server;
  private int port;

  public static class Echo {

    private String name;
    private int count;

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public int getCount () {

      return count;
    }

    public void setCount (int count) {

      this.count = count;
    }
  }

  private record RecordedRequest(String method, String uri, String body, String marker) {

  }

  private static class CapturingAppender extends AbstractAppender {

    private final List<Record<?>> records = new CopyOnWriteArrayList<>();

    @Override
    public void handleOutput (Record<?> record) {

      records.add(record);
    }

    public List<Record<?>> getRecords () {

      return records;
    }
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/echo", this::handleEcho);
    server.createContext("/empty", this::handleEmpty);
    server.createContext("/boom", this::handleBoom);
    server.createContext("/app/x", this::handleEcho);
    server.start();
    port = server.getAddress().getPort();
  }

  @AfterClass(alwaysRun = true)
  public void afterClass () {

    if (server != null) {
      server.stop(0);
    }
  }

  @BeforeMethod
  public void beforeMethod () {

    lastRequestRef.set(null);
  }

  @AfterMethod(alwaysRun = true)
  public void afterMethod () {

    LoggerManager.getLogger(JsonTarget.class).clearAppenders();
    LoggerManager.getLogger(JsonTarget.class).setLevel(Level.INFO);
  }

  private void handleEcho (HttpExchange exchange)
    throws IOException {

    byte[] requestBytes = exchange.getRequestBody().readAllBytes();

    lastRequestRef.set(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(), new String(requestBytes, StandardCharsets.UTF_8), exchange.getRequestHeaders().getFirst("X-Marker")));

    byte[] body = ("{\"name\":\"" + exchange.getRequestMethod() + "\",\"count\":42}").getBytes(StandardCharsets.UTF_8);

    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(body);
    }
  }

  private void handleEmpty (HttpExchange exchange)
    throws IOException {

    lastRequestRef.set(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(), new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), null));

    exchange.sendResponseHeaders(204, -1);
    exchange.close();
  }

  private void handleBoom (HttpExchange exchange)
    throws IOException {

    exchange.getRequestBody().readAllBytes();
    exchange.sendResponseHeaders(404, -1);
    exchange.close();
  }

  private JsonTarget target (String path)
    throws Exception {

    return JsonTargetFactory.manufacture(HttpProtocol.HTTP, "127.0.0.1", port).path(path);
  }

  public void testGetRoundTrip ()
    throws Exception {

    Echo response = target("/echo").get(Echo.class);

    Assert.assertEquals(response.getName(), "GET");
    Assert.assertEquals(response.getCount(), 42);
    Assert.assertEquals(lastRequestRef.get().method(), "GET");
  }

  public void testPutRoundTrip ()
    throws Exception {

    Echo response = target("/echo").put(new JsonBody("{\"name\":\"in\",\"count\":1}"), Echo.class);

    Assert.assertEquals(response.getName(), "PUT");
    Assert.assertEquals(lastRequestRef.get().method(), "PUT");
    Assert.assertEquals(lastRequestRef.get().body(), "{\"name\":\"in\",\"count\":1}");
  }

  public void testPostRoundTrip ()
    throws Exception {

    Echo response = target("/echo").post(new JsonBody("{\"name\":\"in\",\"count\":2}"), Echo.class);

    Assert.assertEquals(response.getName(), "POST");
    Assert.assertEquals(lastRequestRef.get().method(), "POST");
    Assert.assertEquals(lastRequestRef.get().body(), "{\"name\":\"in\",\"count\":2}");
  }

  public void testPatchRoundTrip ()
    throws Exception {

    Echo response = target("/echo").patch(new JsonBody("{\"name\":\"in\",\"count\":3}"), Echo.class);

    Assert.assertEquals(response.getName(), "PATCH");
    Assert.assertEquals(lastRequestRef.get().method(), "PATCH");
  }

  public void testDeleteRoundTrip ()
    throws Exception {

    Echo response = target("/echo").delete(Echo.class);

    Assert.assertEquals(response.getName(), "DELETE");
    Assert.assertEquals(lastRequestRef.get().method(), "DELETE");
  }

  public void testEmptyBodyWith2xxReturnsNull ()
    throws Exception {

    Echo response = target("/empty").get(Echo.class);

    Assert.assertNull(response);
    Assert.assertEquals(lastRequestRef.get().method(), "GET");
  }

  public void testNon2xxThrowsWebApplicationException ()
    throws Exception {

    try {
      target("/boom").get(Echo.class);
      Assert.fail("Expected a WebApplicationException");
    } catch (WebApplicationException webApplicationException) {
      Assert.assertEquals(webApplicationException.getResponse().getStatus(), 404);
    }
  }

  public void testHeaderApplied ()
    throws Exception {

    target("/echo").header("X-Marker", "marker-value").get(Echo.class);

    Assert.assertEquals(lastRequestRef.get().marker(), "marker-value");
  }

  public void testQueryApplied ()
    throws Exception {

    target("/echo").query("alpha", "one").query("beta", "two").get(Echo.class);

    Assert.assertEquals(lastRequestRef.get().uri(), "/echo?alpha=one&beta=two");
  }

  public void testDebugCollectorsLogRequestAndResponse ()
    throws Exception {

    CapturingAppender appender = new CapturingAppender();
    Logger logger = LoggerManager.getLogger(JsonTarget.class);

    logger.clearAppenders();
    logger.addAppender(appender);
    logger.setLevel(Level.TRACE);

    Echo response = target("/echo").header("X-Marker", "trace-me").debug(Level.INFO).get(Echo.class);

    Assert.assertNotNull(response);

    boolean sawRequest = false;
    boolean sawResponse = false;

    for (Record<?> record : appender.getRecords()) {

      String message = record.getMessage();

      if ((message != null) && message.startsWith("Sending client request")) {
        sawRequest = true;
        Assert.assertTrue(message.contains("/echo"));
        Assert.assertTrue(message.contains("X-Marker: trace-me"));
      } else if ((message != null) && message.startsWith("Receiving client response")) {
        sawResponse = true;
        Assert.assertTrue(message.contains("< 200"));
        Assert.assertTrue(message.contains("\"name\":\"GET\""));
      }
    }

    Assert.assertTrue(sawRequest, "Expected a RequestDebugCollector record");
    Assert.assertTrue(sawResponse, "Expected a ResponseDebugCollector record");
  }

  public void testContextPrefixComposesPath ()
    throws Exception {

    Echo response = JsonTargetFactory.manufacture(HttpProtocol.HTTP, "127.0.0.1", port, "/app").path("/x").get(Echo.class);

    Assert.assertNotNull(response);
    Assert.assertEquals(lastRequestRef.get().uri(), "/app/x");
    Assert.assertEquals(response.getName(), "GET");
  }

  public void testTrailingSlashContextCollapses ()
    throws Exception {

    Echo response = JsonTargetFactory.manufacture(HttpProtocol.HTTP, "127.0.0.1", port, "/app/").path("/x").get(Echo.class);

    Assert.assertNotNull(response);
    Assert.assertEquals(lastRequestRef.get().uri(), "/app/x");
  }

  public void testRawTargetUriConstructor ()
    throws Exception {

    JsonTarget rawTarget = new JsonTarget("http://127.0.0.1:" + port);
    Echo response = rawTarget.path("/echo").get(Echo.class);

    Assert.assertEquals(response.getName(), "GET");
    Assert.assertEquals(lastRequestRef.get().uri(), "/echo");
  }

  public void testPojoDeserialization ()
    throws Exception {

    Echo echo = target("/echo").get(Echo.class);

    Assert.assertEquals(echo.getName(), "GET");
    Assert.assertEquals(echo.getCount(), 42);
  }
}
