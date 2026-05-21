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

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpHeader;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies the {@code allowsImplicitConnection} contract end to end on the wire. The flag is
 * consulted by both {@code Meta.SUBSCRIBE} (line 390) and {@code Meta.PUBLISH} (line 583): when a
 * session is in {@code HANDSHOOK} state but not yet {@code CONNECTED}, each handler
 * short-circuits its {@code "Connection required"} rejection and calls
 * {@code session.completeConnection()} before continuing.
 *
 * <p>The unit tests {@code MetaSubscribeTest.testHandshookWithImplicitConnectionAdvancesSession}
 * and {@code MetaPublishTest.testHandshookWithImplicitConnectionAdvancesSession} cover both
 * branches against mocked dependencies; this integration test confirms the matching wire
 * behavior by issuing raw Bayeux POSTs through {@link HttpClient}, skipping the
 * {@code /meta/connect} the cometd {@code BayeuxClient} would normally insert. Two scenarios
 * are pinned:</p>
 *
 * <ul>
 *   <li>subscribe right after handshake succeeds (flag honored on the subscribe path)</li>
 *   <li>publish right after handshake succeeds (flag honored on the publish path)</li>
 * </ul>
 */
@Test(groups = "integration")
public class ImplicitConnectionIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String[] IMPLICIT_SPRING_RESOURCE_LOCATIONS = new String[] {
    "org/smallmind/bayeux/oumuamua/server/impl/logging.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-grizzly.xml",
    "org/smallmind/bayeux/oumuamua/server/impl/oumuamua-implicit.xml"
  };
  private static final String IMPLICIT_SUBSCRIBE_CHANNEL = "/integration/implicit-subscribe";
  private static final String IMPLICIT_PUBLISH_CHANNEL = "/integration/implicit-publish";
  private static final String CONTENT_TYPE = "application/json;charset=UTF-8";
  private static final String PAYLOAD = "implicit-publish-payload";

  private HttpClient httpClient;
  private ObjectMapper objectMapper;

  @Override
  protected String[] springResourceLocations () {

    return IMPLICIT_SPRING_RESOURCE_LOCATIONS;
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();
    objectMapper = new ObjectMapper();
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

  private JsonNode postBayeuxMessages (List<Map<String, Object>> messages)
    throws Exception {

    String body = objectMapper.writeValueAsString(messages);
    ContentResponse response = httpClient.POST(serverUrl())
                                 .headers(headers -> headers.put(HttpHeader.CONTENT_TYPE, CONTENT_TYPE))
                                 .body(new StringRequestContent(CONTENT_TYPE, body))
                                 .send();

    Assert.assertEquals(response.getStatus(), 200, "Bayeux endpoint returned non-200 for body " + body + "; response: " + response.getContentAsString());

    JsonNode root = objectMapper.readTree(response.getContentAsString());

    Assert.assertTrue(root.isArray() && (root.size() >= 1), "Bayeux endpoint did not return a non-empty JSON array; body was " + response.getContentAsString());

    return root;
  }

  private String handshake ()
    throws Exception {

    Map<String, Object> handshakeRequest = Map.of(
      "channel", "/meta/handshake",
      "version", "1.0",
      "minimumVersion", "1.0",
      "supportedConnectionTypes", List.of("long-polling"),
      "id", "1");
    JsonNode reply = postBayeuxMessages(List.of(handshakeRequest)).get(0);

    Assert.assertTrue(reply.path("successful").asBoolean(), "Handshake reply was not successful: " + reply);

    String clientId = reply.path("clientId").asText();

    Assert.assertFalse(clientId.isEmpty(), "Handshake reply did not include a clientId: " + reply);

    return clientId;
  }

  @Test
  public void subscribeWithoutConnectSucceedsWhenImplicitConnectionAllowed ()
    throws Exception {

    String clientId = handshake();
    Map<String, Object> subscribeRequest = Map.of(
      "channel", "/meta/subscribe",
      "clientId", clientId,
      "subscription", IMPLICIT_SUBSCRIBE_CHANNEL,
      "id", "2");
    JsonNode subscribeReply = postBayeuxMessages(List.of(subscribeRequest)).get(0);

    Assert.assertEquals(subscribeReply.path("channel").asText(), "/meta/subscribe", "Subscribe reply on unexpected channel: " + subscribeReply);
    Assert.assertTrue(subscribeReply.path("successful").asBoolean(), "Subscribe without /meta/connect was rejected even though allowsImplicitConnection=true; reply: " + subscribeReply);
    Assert.assertEquals(subscribeReply.path("subscription").asText(), IMPLICIT_SUBSCRIBE_CHANNEL, "Subscribe reply did not echo the subscription path: " + subscribeReply);
  }

  @Test
  public void publishWithoutConnectSucceedsWhenImplicitConnectionAllowed ()
    throws Exception {

    String clientId = handshake();
    Map<String, Object> publishRequest = Map.of(
      "channel", IMPLICIT_PUBLISH_CHANNEL,
      "clientId", clientId,
      "data", PAYLOAD,
      "id", "2");
    JsonNode publishReply = postBayeuxMessages(List.of(publishRequest)).get(0);

    Assert.assertEquals(publishReply.path("channel").asText(), IMPLICIT_PUBLISH_CHANNEL, "Publish reply on unexpected channel: " + publishReply);
    Assert.assertTrue(publishReply.path("successful").asBoolean(), "Publish without /meta/connect was rejected even though allowsImplicitConnection=true; reply: " + publishReply);
  }
}
