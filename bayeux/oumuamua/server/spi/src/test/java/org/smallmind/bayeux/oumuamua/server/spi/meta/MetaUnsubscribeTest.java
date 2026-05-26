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
package org.smallmind.bayeux.oumuamua.server.spi.meta;

import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MetaUnsubscribeTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;
  private Session<OrthodoxValue> session;
  private Protocol<OrthodoxValue> protocol;

  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);
    session = Mockito.mock(Session.class);
    protocol = Mockito.mock(Protocol.class);

    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
  }

  private Message<OrthodoxValue> request (String subscription) {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.SESSION_ID, "alice");
    request.put(Message.CHANNEL, DefaultRoute.UNSUBSCRIBE_ROUTE.getPath());

    if (subscription != null) {
      request.put(Message.SUBSCRIPTION, subscription);
    }

    return request;
  }

  public void testMissingSubscriptionProducesError ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request(null));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Missing subscription");
    Assert.assertNull(response.getAdvice());
  }

  public void testMismatchedSessionIdProducesHandshakeRequired ()
    throws Exception {

    Message<OrthodoxValue> request = request("/foo");

    request.put(Message.SESSION_ID, "bob");

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.HANDSHAKE.getCode());
  }

  public void testPreHandshakeStateProducesHandshakeRequired ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.INITIALIZED);

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
  }

  public void testHandshookButNotConnectedProducesConnectionRequired ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Connection required");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.RETRY.getCode());
  }

  public void testMetaChannelSubscriptionIsRejected ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request("/meta/handshake"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Attempted subscription to a meta channel");
  }

  public void testAbsentChannelProducesSilentSuccess ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(null);

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.SUBSCRIPTION)).asText(), "/foo");
  }

  public void testPresentChannelIsUnsubscribed ()
    throws Exception {

    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Mockito.verify(channel).unsubscribe(session);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.SUBSCRIPTION)).asText(), "/foo");
  }

  public void testNonStringSubscriptionFieldProducesError ()
    throws Exception {

    Message<OrthodoxValue> request = request(null);

    request.put(Message.SUBSCRIPTION, 42);

    Packet<OrthodoxValue> packet = Meta.UNSUBSCRIBE.process(protocol, DefaultRoute.UNSUBSCRIBE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Missing subscription");
  }
}
