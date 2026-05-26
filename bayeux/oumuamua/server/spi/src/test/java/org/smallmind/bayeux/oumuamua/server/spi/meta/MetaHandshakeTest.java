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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.SecurityPolicy;
import org.smallmind.bayeux.oumuamua.server.api.SecurityRejection;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MetaHandshakeTest {

  private OrthodoxCodec codec;
  private OrthodoxValueFactory factory;
  private Server<OrthodoxValue> server;
  private Session<OrthodoxValue> session;
  private Protocol<OrthodoxValue> protocol;

  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    factory = new OrthodoxValueFactory();
    server = Mockito.mock(Server.class);
    session = Mockito.mock(Session.class);
    protocol = Mockito.mock(Protocol.class);

    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(server.getBayeuxVersion()).thenReturn("1.0");
    Mockito.when(server.getMinimumBayeuxVersion()).thenReturn("1.0");
    Mockito.when(server.getProtocolNames()).thenReturn(new String[0]);
    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.getState()).thenReturn(SessionState.INITIALIZED);
    Mockito.when(protocol.getTransportNames()).thenReturn(new String[] {"websocket"});
  }

  private Message<OrthodoxValue> request () {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());
    request.put(Message.SUPPORTED_CONNECTION_TYPES, factory.arrayValue().add(factory.textValue("websocket")));

    return request;
  }

  private Set<String> transportNamesIn (Message<OrthodoxValue> message) {

    ArrayValue<OrthodoxValue> array = (ArrayValue<OrthodoxValue>)message.get(Message.SUPPORTED_CONNECTION_TYPES);
    Set<String> names = new HashSet<>();

    for (int index = 0; index < array.size(); index++) {
      names.add(((StringValue<OrthodoxValue>)array.get(index)).asText());
    }

    return names;
  }

  public void testSecurityRejectionWithReasonProducesUnauthorizedError ()
    throws Exception {

    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(policy.canHandshake(Mockito.eq(session), Mockito.any())).thenReturn(SecurityRejection.reason("nope"));

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized: nope");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.NONE.getCode());
    Mockito.verify(session, Mockito.never()).completeHandshake();
  }

  public void testSecurityRejectionWithoutReasonProducesUnauthorizedError ()
    throws Exception {

    SecurityPolicy<OrthodoxValue> policy = Mockito.mock(SecurityPolicy.class);

    Mockito.when(server.getSecurityPolicy()).thenReturn(policy);
    Mockito.when(policy.canHandshake(Mockito.eq(session), Mockito.any())).thenReturn(SecurityRejection.noReason());

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unauthorized");
  }

  public void testNullSecurityPolicyAllowsHandshakeToProceed ()
    throws Exception {

    Mockito.when(server.getSecurityPolicy()).thenReturn(null);

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Mockito.verify(session).completeHandshake();
  }

  public void testAlreadyHandshookProducesPreviousHandshakeError ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake was previously completed");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.RETRY.getCode());
    Mockito.verify(session, Mockito.never()).completeHandshake();
  }

  public void testAlreadyConnectedAlsoRejectsHandshake ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake was previously completed");
  }

  public void testMissingSupportedConnectionTypesProducesUnsupportedError ()
    throws Exception {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake attempted on an unsupported transport");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.HANDSHAKE.getCode());
  }

  public void testEmptySupportedConnectionTypesProducesUnsupportedError ()
    throws Exception {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());
    request.put(Message.SUPPORTED_CONNECTION_TYPES, factory.arrayValue());

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake attempted on an unsupported transport");
  }

  public void testNonMatchingSupportedConnectionTypesProducesUnsupportedError ()
    throws Exception {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());
    request.put(Message.SUPPORTED_CONNECTION_TYPES, factory.arrayValue().add(factory.textValue("smoke-signals")));

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake attempted on an unsupported transport");
  }

  public void testIntersectionWithAnyProtocolTransportTriggersSuccess ()
    throws Exception {

    Mockito.when(protocol.getTransportNames()).thenReturn(new String[] {"long-polling", "websocket"});

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());
    request.put(Message.SUPPORTED_CONNECTION_TYPES, factory.arrayValue().add(factory.textValue("smoke-signals")).add(factory.textValue("websocket")));

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testSuccessAdvancesSessionState ()
    throws Exception {

    Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());

    Mockito.verify(session).completeHandshake();
  }

  public void testSuccessResponseCarriesVersionFields ()
    throws Exception {

    Mockito.when(server.getBayeuxVersion()).thenReturn("1.0");
    Mockito.when(server.getMinimumBayeuxVersion()).thenReturn("0.9");

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.VERSION)).asText(), "1.0");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.MINIMUM_VERSION)).asText(), "0.9");
  }

  public void testSuccessResponseAdvertisesOnlyProtocolTransports ()
    throws Exception {

    Mockito.when(protocol.getTransportNames()).thenReturn(new String[] {"websocket"});
    Mockito.when(server.getProtocolNames()).thenReturn(new String[] {"other"});

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(transportNamesIn(response), new HashSet<>(Arrays.asList("websocket")));
  }

  public void testErrorResponseAdvertisesAllServerTransports ()
    throws Exception {

    Protocol<OrthodoxValue> alt = Mockito.mock(Protocol.class);

    Mockito.when(alt.getTransportNames()).thenReturn(new String[] {"long-polling"});
    Mockito.when(server.getProtocolNames()).thenReturn(new String[] {"alt"});
    Mockito.when(server.getProtocol("alt")).thenReturn(alt);
    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertTrue(transportNamesIn(response).contains("long-polling"));
  }

  public void testResponsePacketCarriesSessionIdAndHandshakeRoute ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());

    Assert.assertEquals(packet.getPacketType(), PacketType.RESPONSE);
    Assert.assertSame(packet.getRoute(), DefaultRoute.HANDSHAKE_ROUTE);
    Assert.assertEquals(packet.getSenderId(), "alice");
  }

  public void testNullProtocolTransportNamesProducesUnsupportedError ()
    throws Exception {

    Mockito.when(protocol.getTransportNames()).thenReturn(null);

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake attempted on an unsupported transport");
  }

  public void testEmptyProtocolTransportNamesProducesUnsupportedError ()
    throws Exception {

    Mockito.when(protocol.getTransportNames()).thenReturn(new String[0]);

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake attempted on an unsupported transport");
  }

  public void testNonArraySupportedConnectionTypesProducesUnsupportedError ()
    throws Exception {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());
    request.put(Message.SUPPORTED_CONNECTION_TYPES, "websocket");

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake attempted on an unsupported transport");
  }

  public void testNonStringSupportedConnectionTypesElementIsSkipped ()
    throws Exception {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());
    request.put(Message.SUPPORTED_CONNECTION_TYPES, factory.arrayValue().add(factory.numberValue(42)).add(factory.textValue("websocket")));

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testSupportedConnectionTypesContainingOnlyNonStringYieldsUnsupportedError ()
    throws Exception {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.CHANNEL, DefaultRoute.HANDSHAKE_ROUTE.getPath());
    request.put(Message.SUPPORTED_CONNECTION_TYPES, factory.arrayValue().add(factory.numberValue(42)));

    Packet<OrthodoxValue> packet = Meta.HANDSHAKE.process(protocol, DefaultRoute.HANDSHAKE_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake attempted on an unsupported transport");
  }
}
