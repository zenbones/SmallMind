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
package org.smallmind.web.http.apache;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test that drives the shared {@link HttpClient} async pool against an in-process JDK
 * {@link HttpServer} (no external services), verifying the blocking execute/await round trip.
 */
@Test(groups = "integration")
public class HttpClientTest {

  private HttpServer server;
  private int port;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/ping", exchange -> {

      byte[] body = "pong".getBytes(StandardCharsets.UTF_8);

      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream outputStream = exchange.getResponseBody()) {
        outputStream.write(body);
      }
    });
    server.start();
    port = server.getAddress().getPort();
  }

  @AfterClass(alwaysRun = true)
  public void afterClass () {

    if (server != null) {
      server.stop(0);
    }
  }

  public void testExecuteReturnsResponse ()
    throws Exception {

    SimpleHttpRequest request = SimpleHttpRequest.create("GET", URI.create("http://127.0.0.1:" + port + "/ping"));
    SimpleCallback callback = new SimpleCallback();

    HttpClient.execute(request, callback, 10);

    SimpleHttpResponse response = callback.getResponse();

    Assert.assertEquals(response.getCode(), 200);
    Assert.assertEquals(response.getBodyText(), "pong");
  }
}
