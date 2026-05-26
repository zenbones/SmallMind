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

import java.util.concurrent.TimeUnit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
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
public class MetaConnectTest {

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
    Mockito.when(server.getSessionConnectionIntervalMilliseconds()).thenReturn(0L);
    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
    Mockito.when(session.isLongPolling()).thenReturn(false);
    Mockito.when(protocol.getTransportNames()).thenReturn(new String[] {"websocket"});
    Mockito.when(protocol.getLongPollTimeoutMilliseconds()).thenReturn(0L);
  }

  private Message<OrthodoxValue> request () {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.SESSION_ID, "alice");
    request.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
    request.put(Message.CONNECTION_TYPE, "websocket");

    return request;
  }

  public void testMismatchedSessionIdProducesHandshakeRequired ()
    throws Exception {

    Message<OrthodoxValue> request = request();

    request.put(Message.SESSION_ID, "bob");

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.HANDSHAKE.getCode());
    Assert.assertEquals(packet.getSenderId(), "bob");
  }

  public void testPreHandshakeStateProducesHandshakeRequired ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.INITIALIZED);

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Handshake required");
  }

  public void testHandshookButUnsupportedTransportProducesUnsupportedError ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Message<OrthodoxValue> request = request();

    request.put(Message.CONNECTION_TYPE, "smoke-signals");

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Connection requested on an unsupported transport");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), Reconnect.HANDSHAKE.getCode());
  }

  public void testConnectedSessionSkipsTransportCheck ()
    throws Exception {

    // Already-connected sessions don't re-validate the transport since the connection is established.
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);

    Message<OrthodoxValue> request = request();

    request.put(Message.CONNECTION_TYPE, "smoke-signals");

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testNonLongPollingProducesImmediateResponseWithInterval ()
    throws Exception {

    Mockito.when(server.getSessionConnectionIntervalMilliseconds()).thenReturn(30000L);

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((NumberValue<OrthodoxValue>)response.getAdvice().get(Advice.INTERVAL.getField())).asLong(), 30000L);
    Mockito.verify(session, Mockito.never()).poll(Mockito.anyLong(), Mockito.any());
  }

  public void testFirstConnectAdvancesSessionToConnected ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    Mockito.verify(session).completeConnection();
  }

  public void testAlreadyConnectedDoesNotCallCompleteConnection ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);

    Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    Mockito.verify(session, Mockito.never()).completeConnection();
  }

  public void testLongPollingEmptyQueueProducesSingleResponseMessage ()
    throws Exception {

    Mockito.when(session.isLongPolling()).thenReturn(true);
    Mockito.when(session.poll(Mockito.anyLong(), Mockito.any())).thenReturn(null);

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    Assert.assertEquals(packet.getMessages().length, 1);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)packet.getMessages()[0].get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testLongPollingResponseAdviceIntervalIsZero ()
    throws Exception {

    Mockito.when(session.isLongPolling()).thenReturn(true);
    Mockito.when(server.getSessionConnectionIntervalMilliseconds()).thenReturn(30000L);
    Mockito.when(session.poll(Mockito.anyLong(), Mockito.any())).thenAnswer(invocation -> {
      Mockito.when(session.getState()).thenReturn(SessionState.DISCONNECTED);

      return null;
    });

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    Assert.assertEquals(((NumberValue<OrthodoxValue>)packet.getMessages()[0].getAdvice().get(Advice.INTERVAL.getField())).asLong(), 0L);
  }

  public void testLongPollingPrependsResponseAndAppendsQueuedMessages ()
    throws Exception {

    Mockito.when(session.isLongPolling()).thenReturn(true);

    Message<OrthodoxValue> deliveryMessage = codec.create();

    deliveryMessage.put(Message.CHANNEL, "/foo");

    Packet<OrthodoxValue> queuedPacket = new Packet<>(PacketType.DELIVERY, "publisher", DefaultRoute.CONNECT_ROUTE, deliveryMessage);

    Mockito.when(session.poll(Mockito.anyLong(), Mockito.any())).thenReturn(queuedPacket, (Packet<OrthodoxValue>)null);

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    Assert.assertEquals(packet.getMessages().length, 2);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)packet.getMessages()[0].get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertSame(packet.getMessages()[1], deliveryMessage);
  }

  public void testLongPollingHonorsAdviceTimeoutAsFirstPollDuration ()
    throws Exception {

    Mockito.when(session.isLongPolling()).thenReturn(true);
    Mockito.when(session.poll(Mockito.anyLong(), Mockito.any())).thenAnswer(invocation -> {
      Mockito.when(session.getState()).thenReturn(SessionState.DISCONNECTED);

      return null;
    });

    Message<OrthodoxValue> request = request();

    request.put(Message.ADVICE, factory.objectValue().put(Advice.TIMEOUT.getField(), 50L));

    Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request);

    ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);

    Mockito.verify(session, Mockito.atLeastOnce()).poll(timeoutCaptor.capture(), Mockito.eq(TimeUnit.MILLISECONDS));
    Assert.assertEquals(timeoutCaptor.getAllValues().get(0).longValue(), 50L);
  }

  public void testLongPollingUsesSessionConnectIntervalAsFirstPollWhenNoAdvice ()
    throws Exception {

    Mockito.when(session.isLongPolling()).thenReturn(true);
    Mockito.when(server.getSessionConnectionIntervalMilliseconds()).thenReturn(50L);
    Mockito.when(session.poll(Mockito.anyLong(), Mockito.any())).thenAnswer(invocation -> {
      Mockito.when(session.getState()).thenReturn(SessionState.DISCONNECTED);

      return null;
    });

    Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);

    Mockito.verify(session, Mockito.atLeastOnce()).poll(timeoutCaptor.capture(), Mockito.eq(TimeUnit.MILLISECONDS));
    Assert.assertEquals(timeoutCaptor.getAllValues().get(0).longValue(), 50L);
  }

  public void testLongPollingExitsWhenSessionLeavesConnectedState ()
    throws Exception {

    Mockito.when(session.isLongPolling()).thenReturn(true);
    Mockito.when(server.getSessionConnectionIntervalMilliseconds()).thenReturn(1000L);
    Mockito.when(session.poll(Mockito.anyLong(), Mockito.any())).thenAnswer(invocation -> {
      Mockito.when(session.getState()).thenReturn(SessionState.DISCONNECTED);

      return null;
    });

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    Assert.assertEquals(packet.getMessages().length, 1);
    Mockito.verify(session, Mockito.times(1)).poll(Mockito.anyLong(), Mockito.any());
  }

  public void testResponsePacketCarriesSessionIdAndConnectRoute ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());

    Assert.assertEquals(packet.getPacketType(), PacketType.RESPONSE);
    Assert.assertSame(packet.getRoute(), DefaultRoute.CONNECT_ROUTE);
    Assert.assertEquals(packet.getSenderId(), "alice");
  }

  public void testMissingConnectionTypeOnFirstConnectProducesUnsupportedTransport ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.SESSION_ID, "alice");
    request.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Connection requested on an unsupported transport");
  }

  public void testNonStringConnectionTypeOnFirstConnectProducesUnsupportedTransport ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);

    Message<OrthodoxValue> request = request();

    request.put(Message.CONNECTION_TYPE, 42);

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request);
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Connection requested on an unsupported transport");
  }

  public void testNullProtocolTransportNamesOnFirstConnectProducesUnsupportedTransport ()
    throws Exception {

    Mockito.when(session.getState()).thenReturn(SessionState.HANDSHOOK);
    Mockito.when(protocol.getTransportNames()).thenReturn(null);

    Packet<OrthodoxValue> packet = Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Connection requested on an unsupported transport");
  }

  public void testNonNumberAdviceTimeoutFallsBackToSessionConnectInterval ()
    throws Exception {

    Mockito.when(session.isLongPolling()).thenReturn(true);
    Mockito.when(server.getSessionConnectionIntervalMilliseconds()).thenReturn(50L);
    Mockito.when(session.poll(Mockito.anyLong(), Mockito.any())).thenAnswer(invocation -> {
      Mockito.when(session.getState()).thenReturn(SessionState.DISCONNECTED);

      return null;
    });

    Message<OrthodoxValue> request = request();

    request.put(Message.ADVICE, factory.objectValue().put(Advice.TIMEOUT.getField(), "not-a-number"));

    Meta.CONNECT.process(protocol, DefaultRoute.CONNECT_ROUTE, server, session, request);

    ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);

    Mockito.verify(session, Mockito.atLeastOnce()).poll(timeoutCaptor.capture(), Mockito.eq(TimeUnit.MILLISECONDS));
    Assert.assertEquals(timeoutCaptor.getAllValues().get(0).longValue(), 50L);
  }
}
