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

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.SecurityRejection;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MetaPublishTest {

  private OrthodoxCodec codec;
  private OrthodoxValueFactory factory;
  private Server<OrthodoxValue> server;
  private Session<OrthodoxValue> session;
  private Channel<OrthodoxValue> channel;
  private TestProtocol protocol;
  private Route route;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod ()
    throws Exception {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    factory = new OrthodoxValueFactory();
    server = Mockito.mock(Server.class);
    session = Mockito.mock(Session.class);
    channel = Mockito.mock(Channel.class);
    protocol = new TestProtocol();
    route = new DefaultRoute("/foo");

    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
  }

  private Message<OrthodoxValue> request () {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.SESSION_ID, "alice");
    request.put(Message.CHANNEL, "/foo");
    request.put(Message.DATA, factory.objectValue().put("payload", "hi"));

    return request;
  }

  public void testMismatchedSessionIdProducesHandshakeRequired ()
    throws Exception {

    Message<OrthodoxValue> request = request();

    request.put(Message.SESSION_ID, "bob");

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.HANDSHAKE.getCode());
  }

  public void testPreHandshakeStateProducesHandshakeRequired ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.INITIALIZED);

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
  }

  public void testHandshookButNotConnectedProducesConnectionRequired ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Connection required");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.RETRY.getCode());
  }

  public void testHandshookWithImplicitConnectionAdvancesSession ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);
    Mockito.when(server.allowsImplicitConnection()).thenReturn(true);
    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Mockito.verify(session).completeConnection();
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testMetaChannelRouteIsRejected ()
    throws Exception {

    Route metaRoute = new DefaultRoute("/meta/custom");

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, metaRoute, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Attempted to publish to a meta channel");
  }

  public void testInvalidPathProducesError ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenThrow(new InvalidPathException("bad path"));

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "bad path");
  }

  public void testCanCreateRejectionProducesUnauthorized ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(null);
    Mockito.when(policy.canCreate(Mockito.eq(session), Mockito.eq("/foo"), Mockito.any())).thenReturn(SecurityRejection.reason("no creates"));

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized: no creates");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.NONE.getCode());
    Mockito.verify(server, Mockito.never()).requireChannel(Mockito.anyString());
  }

  public void testCanPublishRejectionProducesUnauthorized ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(channel);
    Mockito.when(policy.canPublish(Mockito.eq(session), Mockito.eq(channel), Mockito.any())).thenReturn(SecurityRejection.reason("no publishes"));

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized: no publishes");
    Mockito.verify(server, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
  }

  public void testCanCreateRejectionWithoutReasonProducesPlainUnauthorized ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(null);
    Mockito.when(policy.canCreate(Mockito.eq(session), Mockito.eq("/foo"), Mockito.any())).thenReturn(SecurityRejection.noReason());

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized");
    Mockito.verify(server, Mockito.never()).requireChannel(Mockito.anyString());
  }

  public void testCanPublishRejectionWithoutReasonProducesPlainUnauthorized ()
    throws Exception {

    @SuppressWarnings("unchecked")
    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(server.findChannel("/foo")).thenReturn(channel);
    Mockito.when(policy.canPublish(Mockito.eq(session), Mockito.eq(channel), Mockito.any())).thenReturn(SecurityRejection.noReason());

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized");
    Mockito.verify(server, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
  }

  @SuppressWarnings("unchecked")
  public void testSuccessfulPublishDeliversAndEchoesRequest ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Message<OrthodoxValue> request = request();
    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request);

    ArgumentCaptor<Packet<OrthodoxValue>> deliveryPacket = ArgumentCaptor.forClass(Packet.class);

    Mockito.verify(server).deliver(Mockito.eq(session), deliveryPacket.capture(), Mockito.eq(true));

    Assert.assertEquals(deliveryPacket.getValue().getPacketType(), PacketType.DELIVERY);
    Assert.assertEquals(deliveryPacket.getValue().getMessages()[0].getChannel(), "/foo");
    Assert.assertEquals(packet.getMessages().length, 2);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)packet.getMessages()[0].get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertSame(packet.getMessages()[1], request);
  }

  public void testExplicitEchoFalseSuppressesRequestInResponse ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Message<OrthodoxValue> request = request();

    request.getExt(true).put("oumuamua", factory.objectValue().put("echo", false));

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request);

    Assert.assertEquals(packet.getMessages().length, 1);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)packet.getMessages()[0].get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testExplicitEchoTrueKeepsRequestInResponse ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Message<OrthodoxValue> request = request();

    request.getExt(true).put("oumuamua", factory.objectValue().put("echo", true));

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request);

    Assert.assertEquals(packet.getMessages().length, 2);
    Assert.assertSame(packet.getMessages()[1], request);
  }

  public void testProtocolListenerReceivesPublishCallback ()
    throws Exception {

    RecordingListener listener = new RecordingListener();

    protocol.addListener(listener);
    Mockito.when(server.findChannel("/foo")).thenReturn(channel);

    Meta.PUBLISH.process(protocol, route, server, session, request());

    Assert.assertEquals(listener.publishCalls, 1);
  }

  public void testAbsentChannelTriggersCreation ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(null);
    Mockito.when(server.requireChannel("/foo")).thenReturn(channel);

    Meta.PUBLISH.process(protocol, route, server, session, request());

    Mockito.verify(server).requireChannel("/foo");
    Mockito.verify(server).deliver(Mockito.eq(session), Mockito.any(), Mockito.eq(true));
  }

  public void testExceptionDuringDeliveryWrappedAsErrorResponse ()
    throws Exception {

    Mockito.when(server.findChannel("/foo")).thenReturn(channel);
    Mockito.doThrow(new RuntimeException("kaboom")).when(server).deliver(Mockito.any(), Mockito.any(), Mockito.anyBoolean());

    Packet<OrthodoxValue> packet = Meta.PUBLISH.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "kaboom");
  }

  private static class TestProtocol extends AbstractProtocol<OrthodoxValue> {

    @Override
    public String getName () {

      return "test";
    }

    @Override
    public boolean isLongPolling () {

      return false;
    }

    @Override
    public long getLongPollTimeoutMilliseconds () {

      return 0L;
    }

    @Override
    public String[] getTransportNames () {

      return new String[] {"test"};
    }

    @Override
    public Transport<OrthodoxValue> getTransport (String name) {

      return null;
    }
  }

  private static class RecordingListener implements org.smallmind.bayeux.oumuamua.server.api.Protocol.ProtocolListener<OrthodoxValue> {

    private int publishCalls;

    @Override
    public void onReceipt (Message<OrthodoxValue>[] incomingMessages) {

    }

    @Override
    public void onPublish (Message<OrthodoxValue> originatingMessage, Message<OrthodoxValue> outgoingMessage) {

      publishCalls++;
    }

    @Override
    public void onDelivery (Packet<OrthodoxValue> outgoingPacket) {

    }
  }
}
