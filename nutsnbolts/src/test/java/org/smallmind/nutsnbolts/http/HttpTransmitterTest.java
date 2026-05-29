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
package org.smallmind.nutsnbolts.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class HttpTransmitterTest {

  private HttpServer server;
  private RecordingHandler handler;
  private String baseUrl;

  @BeforeClass
  public void startServer ()
    throws IOException {

    handler = new RecordingHandler();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/echo", handler);
    server.setExecutor(null);
    server.start();

    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @AfterClass(alwaysRun = true)
  public void stopServer () {

    if (server != null) {
      server.stop(0);
    }
  }

  public void testGetRequestReadsResponseBody ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("hello-get");

    HttpPipe pipe = HttpTransmitter.emitGetRequest(URI.create(baseUrl + "/echo").toURL(), true)
                      .setRequestHeader("X-Test", "value")
                      .connect();

    Assert.assertEquals(readAll(pipe), "hello-get");
    Assert.assertEquals(handler.lastMethod(), "GET");
    Assert.assertEquals(handler.lastHeader("X-Test"), "value");
  }

  public void testPostRequestSendsBodyAndReadsResponse ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("ok");

    HttpPipe pipe = HttpTransmitter.emitPostRequest(URI.create(baseUrl + "/echo").toURL(), true).connect();

    pipe.write("payload");
    pipe.doneWriting();

    Assert.assertEquals(readAll(pipe), "ok");
    Assert.assertEquals(handler.lastMethod(), "POST");
    Assert.assertEquals(handler.lastBody(), "payload");
  }

  public void testPutRequestSendsBody ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("done");

    HttpPipe pipe = HttpTransmitter.emitPutRequest(URI.create(baseUrl + "/echo").toURL(), true).connect();

    pipe.write("put-body");
    pipe.doneWriting();

    Assert.assertEquals(readAll(pipe), "done");
    Assert.assertEquals(handler.lastMethod(), "PUT");
    Assert.assertEquals(handler.lastBody(), "put-body");
  }

  public void testDeleteRequestReadsResponse ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("gone");

    HttpPipe pipe = HttpTransmitter.emitDeleteRequest(URI.create(baseUrl + "/echo").toURL(), true).connect();

    Assert.assertEquals(readAll(pipe), "gone");
    Assert.assertEquals(handler.lastMethod(), "DELETE");
  }

  public void testGenericEmitHttpRequestRoutesByMethod ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("any");

    HttpPipe pipe = HttpTransmitter.emitHttpRequest(HttpMethod.GET, URI.create(baseUrl + "/echo").toURL(), true).connect();

    Assert.assertEquals(readAll(pipe), "any");
    Assert.assertEquals(handler.lastMethod(), "GET");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testWriteFromGetStateThrows ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("body");

    HttpPipe pipe = HttpTransmitter.emitGetRequest(URI.create(baseUrl + "/echo").toURL(), true).connect();

    pipe.write("not-allowed");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testReadFromWriteStateThrows ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("body");

    HttpPipe pipe = HttpTransmitter.emitPostRequest(URI.create(baseUrl + "/echo").toURL(), true).connect();

    pipe.read(new byte[16]);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testDoubleConnectThrows ()
    throws IOException {

    handler.reset();
    handler.setResponseBody("body");

    HttpPipe pipe = HttpTransmitter.emitGetRequest(URI.create(baseUrl + "/echo").toURL(), true).connect();

    pipe.connect();
  }

  private static String readAll (HttpPipe pipe)
    throws IOException {

    byte[] buffer = new byte[256];
    ByteArrayOutputStream accumulator = new ByteArrayOutputStream();
    int bytesRead;

    while ((bytesRead = pipe.read(buffer)) >= 0) {
      accumulator.write(buffer, 0, bytesRead);
    }

    return accumulator.toString(StandardCharsets.UTF_8);
  }

  private static class RecordingHandler implements HttpHandler {

    private final ConcurrentLinkedQueue<Exchange> exchanges = new ConcurrentLinkedQueue<>();
    private volatile String responseBody = "";

    void setResponseBody (String body) {

      this.responseBody = body;
    }

    void reset () {

      exchanges.clear();
    }

    String lastMethod () {

      Exchange last = lastExchange();

      return (last == null) ? null : last.method;
    }

    String lastBody () {

      Exchange last = lastExchange();

      return (last == null) ? null : last.body;
    }

    String lastHeader (String name) {

      Exchange last = lastExchange();

      return (last == null) ? null : last.headers.get(name);
    }

    private Exchange lastExchange () {

      Exchange found = null;

      for (Exchange exchange : exchanges) {
        found = exchange;
      }

      return found;
    }

    @Override
    public void handle (HttpExchange httpExchange)
      throws IOException {

      Exchange exchange = new Exchange();

      exchange.method = httpExchange.getRequestMethod();

      String headerName = "X-Test";
      String headerValue = httpExchange.getRequestHeaders().getFirst(headerName);

      if (headerValue != null) {
        exchange.headers.put(headerName, headerValue);
      }

      try (InputStream requestStream = httpExchange.getRequestBody()) {

        ByteArrayOutputStream accumulator = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int bytesRead;

        while ((bytesRead = requestStream.read(buffer)) >= 0) {
          accumulator.write(buffer, 0, bytesRead);
        }

        exchange.body = accumulator.toString(StandardCharsets.UTF_8);
      }

      byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

      httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length == 0 ? -1 : responseBytes.length);

      if (responseBytes.length > 0) {
        httpExchange.getResponseBody().write(responseBytes);
      }

      httpExchange.close();
      exchanges.add(exchange);
    }
  }

  private static class Exchange {

    private final HashMap<String, String> headers = new HashMap<>();
    private String method;
    private String body;
  }
}
