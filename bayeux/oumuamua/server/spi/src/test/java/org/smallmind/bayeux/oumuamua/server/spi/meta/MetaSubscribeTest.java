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
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.SecurityRejection;
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
public class MetaSubscribeTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;
  private Session<OrthodoxValue> session;
  private Protocol<OrthodoxValue> protocol;
  private Channel<OrthodoxValue> channel;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod ()
    throws Exception {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);
    session = Mockito.mock(Session.class);
    protocol = Mockito.mock(Protocol.class);
    channel = Mockito.mock(Channel.class);

    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
    Mockito.when(channel.subscribe(session)).thenReturn(true);
  }

  private Message<OrthodoxValue> request (String subscription) {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.SESSION_ID, "alice");
    request.put(Message.CHANNEL, DefaultRoute.SUBSCRIBE_ROUTE.getPath());

    if (subscription != null) {
      request.put(Message.SUBSCRIPTION, subscription);
    }

    return request;
  }

  public void testMissingSubscriptionProducesError ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request(null));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Missing subscription");
  }

  public void testMismatchedSessionIdProducesHandshakeRequired ()
    throws Exception {

    Message<OrthodoxValue> request = request("/foo");

    request.put(Message.SESSION_ID, "bob");

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.HANDSHAKE.getCode());
  }

  public void testPreHandshakeStateProducesHandshakeRequired ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.INITIALIZED);

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
  }

  public void testHandshookWithoutImplicitConnectionProducesConnectionRequired ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);
    Mockito.when(server.allowsImplicitConnection()).thenReturn(false);

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Connection required");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.RETRY.getCode());
  }

  public void testHandshookWithImplicitConnectionAdvancesSession ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);
    Mockito.when(server.allowsImplicitConnection()).thenReturn(true);
    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Mockito.verify(session).completeConnection();
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testMetaChannelSubscriptionIsRejected ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/meta/handshake"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Attempted subscription to a meta channel");
  }

  public void testInvalidPathProducesError ()
    throws Exception {

    Mockito.when(server.findChannel("/bad path")).thenThrow(new InvalidPathException("bad path"));

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/bad path"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "bad path");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.SUBSCRIPTION)).asText(), "/bad path");
  }

  public void testExistingChannelIsSubscribedWithoutCreation ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Mockito.verify(channel).subscribe(session);
    Mockito.verify(server, Mockito.never()).requireChannel(Mockito.anyString());
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.SUBSCRIPTION)).asText(), "/foo");
  }

  public void testCanCreateRejectionWithReasonProducesUnauthorizedWithReason ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(null);
    Mockito.when(policy.canCreate(Mockito.eq(session), Mockito.eq("/foo"), Mockito.any())).thenReturn(SecurityRejection.reason("no creates"));

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized: no creates");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.NONE.getCode());
    Mockito.verify(server, Mockito.never()).requireChannel(Mockito.anyString());
  }

  public void testCanCreateRejectionWithoutReasonProducesPlainUnauthorized ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(null);
    Mockito.when(policy.canCreate(Mockito.eq(session), Mockito.eq("/foo"), Mockito.any())).thenReturn(SecurityRejection.noReason());

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized");
  }

  public void testAbsentChannelTriggersCreationWhenAllowed ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(null);
    Mockito.when(server.requireChannel("/foo")).thenReturn(channel);

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Mockito.verify(server).requireChannel("/foo");
    Mockito.verify(channel).subscribe(session);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testCanSubscribeRejectionProducesUnauthorized ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(channel);
    Mockito.when(policy.canSubscribe(Mockito.eq(session), Mockito.eq(channel), Mockito.any())).thenReturn(SecurityRejection.reason("denied"));

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized: denied");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.NONE.getCode());
    Mockito.verify(channel, Mockito.never()).subscribe(session);
  }

  public void testCanSubscribeRejectionWithoutReasonProducesPlainUnauthorized ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(channel);
    Mockito.when(policy.canSubscribe(Mockito.eq(session), Mockito.eq(channel), Mockito.any())).thenReturn(SecurityRejection.noReason());

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized");
    Mockito.verify(channel, Mockito.never()).subscribe(session);
  }

  public void testFailedSubscribeProducesClosedChannelError ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);
    Mockito.when(channel.subscribe(session)).thenReturn(false);

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Attempted subscription to a closed channel");
  }

  public void testSuccessResponseCarriesSubscriptionField ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Packet<OrthodoxValue> packet = Meta.SUBSCRIBE.process(protocol, DefaultRoute.SUBSCRIBE_ROUTE, server, session, request("/foo"));
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.SUBSCRIPTION)).asText(), "/foo");
  }
}
