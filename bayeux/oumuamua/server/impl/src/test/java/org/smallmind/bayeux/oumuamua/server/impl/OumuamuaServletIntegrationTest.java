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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * End-to-end verification of the synchronous {@link OumuamuaServlet} using raw Bayeux JSON over
 * plain HTTP rather than the CometD client stack. This is the sync counterpart to
 * {@link AsyncOumuamuaServletIntegrationTest} and exercises the full {@code doPost} path through
 * the servlet, the codec, {@code Server.onRequest}, and {@code LongPollingConnection.onMessages()}.
 *
 * <p>The default Spring locations (inherited from {@link AbstractBayeuxIntegrationTest}) wire the
 * synchronous {@code OumuamuaServlet} at {@code /smallmind/cometd}, so this test does not override
 * {@link #springResourceLocations()}.
 *
 * <p>Two tests are run:
 * <ul>
 *   <li>a missing Content-Length request, which exercises the early-failure branch</li>
 *   <li>a well-formed handshake array, which exercises the full decode and dispatch path</li>
 * </ul>
 */
@Test(groups = "integration")
public class OumuamuaServletIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String HANDSHAKE_MESSAGE =
    "{\"channel\":\"/meta/handshake\",\"version\":\"1.0\",\"minimumVersion\":\"1.0\",\"supportedConnectionTypes\":[\"long-polling\"]}";

  private HttpClient httpClient;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();
    httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  @Test
  public void syncServletHandlesSingleHandshake ()
    throws IOException, InterruptedException {

    HttpResponse<String> response = postCometd("[" + HANDSHAKE_MESSAGE + "]");

    Assert.assertEquals(response.statusCode(), 200, "Expected HTTP 200 from sync servlet");
    Assert.assertTrue(response.body().contains("\"successful\":true"), "Handshake was not successful; body: " + response.body());
    Assert.assertTrue(response.body().contains("/meta/handshake"), "Response did not echo the handshake channel; body: " + response.body());
  }

  @Test
  public void syncServletHandlesBatchedHandshakes ()
    throws IOException, InterruptedException {

    HttpResponse<String> response = postCometd("[" + HANDSHAKE_MESSAGE + "," + HANDSHAKE_MESSAGE + "]");

    Assert.assertEquals(response.statusCode(), 200, "Expected HTTP 200 from sync servlet");

    String body = response.body();

    Assert.assertTrue(body.startsWith("["), "Response should be a JSON array; body: " + body);
    Assert.assertEquals(countOccurrences(body, "\"successful\":true"), 2, "Expected two successful handshake responses; body: " + body);
  }

  @Test
  public void syncServletRejectsEmptyBodyWithBadRequest ()
    throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(serverUrl()))
      .header("Content-Type", "application/json;charset=UTF-8")
      .POST(HttpRequest.BodyPublishers.noBody())
      .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    Assert.assertEquals(response.statusCode(), 400, "Expected HTTP 400 for empty request body");
  }

  private HttpResponse<String> postCometd (String jsonBody)
    throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(serverUrl()))
      .header("Content-Type", "application/json;charset=UTF-8")
      .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
      .build();

    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private int countOccurrences (String text, String substring) {

    int count = 0;
    int index = 0;

    while ((index = text.indexOf(substring, index)) != -1) {
      count++;
      index += substring.length();
    }

    return count;
  }
}
