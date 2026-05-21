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
package org.smallmind.bayeux.oumuamua.server.impl.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.impl.OumuamuaServer;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356.WebSocketTransport;
import org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356.WebsocketProtocol;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link WebSocketEndpoint} that cover the JSR-356 lifecycle entry points
 * ({@code onOpen}, {@code onError}, {@code onCleanup}) and the basic accessor contract. The
 * full message decode and dispatch path is exercised by the WebSocket integration tests; here we
 * verify that the endpoint applies configuration, exposes session identity and transport, and
 * tolerates errors and idempotent cleanup without propagating exceptions.
 */
@Test(groups = "unit")
public class WebSocketEndpointTest {

  private OrthodoxCodec codec;
  private WebSocketEndpoint<OrthodoxValue> endpoint;
  private Session websocketSession;
  private EndpointConfig endpointConfig;
  private OumuamuaServer<OrthodoxValue> server;
  private WebSocketTransport<OrthodoxValue> transport;
  private WebSocketContainer container;

  @SuppressWarnings("unchecked")
  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    endpoint = new WebSocketEndpoint<>();
    websocketSession = Mockito.mock(Session.class);
    endpointConfig = Mockito.mock(EndpointConfig.class);
    server = Mockito.mock(OumuamuaServer.class);
    transport = Mockito.mock(WebSocketTransport.class);
    container = Mockito.mock(WebSocketContainer.class);

    Map<String, Object> userProperties = new HashMap<>();

    userProperties.put(Server.ATTRIBUTE, server);
    userProperties.put(WebSocketTransport.ATTRIBUTE, transport);

    Mockito.when(endpointConfig.getUserProperties()).thenReturn(userProperties);
    Mockito.when(websocketSession.getContainer()).thenReturn(container);
  }

  public void testOnOpenAppliesIdleTimeoutWhenPositive () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(15_000L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);

    endpoint.onOpen(websocketSession, endpointConfig);

    Mockito.verify(websocketSession).setMaxIdleTimeout(15_000L);
    Mockito.verify(container, Mockito.never()).setDefaultMaxTextMessageBufferSize(Mockito.anyInt());
  }

  public void testOnOpenSkipsIdleTimeoutWhenNegative () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(0);

    endpoint.onOpen(websocketSession, endpointConfig);

    Mockito.verify(websocketSession, Mockito.never()).setMaxIdleTimeout(Mockito.anyLong());
  }

  public void testOnOpenAppliesTextBufferSizeWhenPositive () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(65_536);

    endpoint.onOpen(websocketSession, endpointConfig);

    Mockito.verify(container).setDefaultMaxTextMessageBufferSize(65_536);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testOnOpenRegistersWholeMessageHandler () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);

    endpoint.onOpen(websocketSession, endpointConfig);

    ArgumentCaptor<MessageHandler> captor = ArgumentCaptor.forClass(MessageHandler.class);

    Mockito.verify(websocketSession).addMessageHandler(captor.capture());
    Assert.assertSame(captor.getValue(), endpoint);
  }

  public void testGetIdDelegatesToWebsocketSession () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);
    Mockito.when(websocketSession.getId()).thenReturn("ws-session-123");

    endpoint.onOpen(websocketSession, endpointConfig);

    Assert.assertEquals(endpoint.getId(), "ws-session-123");
  }

  public void testGetTransportReturnsConfiguredTransport () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);

    endpoint.onOpen(websocketSession, endpointConfig);

    Transport<OrthodoxValue> actual = endpoint.getTransport();

    Assert.assertSame(actual, transport);
  }

  public void testOnErrorLogsAndDoesNotPropagate () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);

    endpoint.onOpen(websocketSession, endpointConfig);

    endpoint.onError(websocketSession, new RuntimeException("simulated transport failure"));
  }

  public void testOnCleanupClosesOpenSession ()
    throws IOException {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);
    Mockito.when(websocketSession.isOpen()).thenReturn(true);

    endpoint.onOpen(websocketSession, endpointConfig);

    endpoint.onCleanup();

    Mockito.verify(websocketSession).close();
  }

  public void testOnCleanupSkipsAlreadyClosedSession ()
    throws IOException {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);
    Mockito.when(websocketSession.isOpen()).thenReturn(false);

    endpoint.onOpen(websocketSession, endpointConfig);

    endpoint.onCleanup();

    Mockito.verify(websocketSession, Mockito.never()).close();
  }

  public void testOnCleanupSwallowsIoExceptionFromClose ()
    throws IOException {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);
    Mockito.when(websocketSession.isOpen()).thenReturn(true);
    Mockito.doThrow(new IOException("close failed")).when(websocketSession).close();

    endpoint.onOpen(websocketSession, endpointConfig);

    endpoint.onCleanup();
  }

  private Packet<OrthodoxValue> deliveryPacket (String channel)
    throws Exception {

    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    return new Packet<>(PacketType.DELIVERY, null, new DefaultRoute(channel), message);
  }

  private void openEndpoint () {

    Mockito.when(transport.getMaxIdleTimeoutMilliseconds()).thenReturn(-1L);
    Mockito.when(transport.getMaximumTextMessageBufferSize()).thenReturn(-1);
    Mockito.when(server.getMessageLogLevel()).thenReturn(Level.OFF);

    endpoint.onOpen(websocketSession, endpointConfig);
  }

  public void testDeliverDoesNothingWhenSessionIsClosed ()
    throws Exception {

    openEndpoint();

    Mockito.when(websocketSession.isOpen()).thenReturn(false);

    endpoint.deliver(deliveryPacket("/closed"));

    Mockito.verify(websocketSession, Mockito.never()).getBasicRemote();
    Mockito.verify(websocketSession, Mockito.never()).getAsyncRemote();
  }

  @SuppressWarnings("unchecked")
  public void testDeliverUsesSyncSendWhenNoAsyncTimeout ()
    throws Exception {

    WebsocketProtocol<OrthodoxValue> wsProtocol = Mockito.mock(WebsocketProtocol.class);
    RemoteEndpoint.Basic basicRemote = Mockito.mock(RemoteEndpoint.Basic.class);

    Mockito.when(websocketSession.isOpen()).thenReturn(true);
    Mockito.when(websocketSession.getBasicRemote()).thenReturn(basicRemote);
    Mockito.when(transport.getAsyncSendTimeoutMilliseconds()).thenReturn(0L);
    Mockito.when(transport.getProtocol()).thenReturn(wsProtocol);

    openEndpoint();

    Packet<OrthodoxValue> packet = deliveryPacket("/sync");

    endpoint.deliver(packet);

    Mockito.verify(basicRemote).sendText(Mockito.anyString());
    Mockito.verify(wsProtocol).onDelivery(packet);
  }

  @SuppressWarnings("unchecked")
  public void testDeliverUsesAsyncSendWhenTimeoutConfigured ()
    throws Exception {

    WebsocketProtocol<OrthodoxValue> wsProtocol = Mockito.mock(WebsocketProtocol.class);
    RemoteEndpoint.Async asyncRemote = Mockito.mock(RemoteEndpoint.Async.class);
    Future<Void> sendFuture = Mockito.mock(Future.class);

    Mockito.when(websocketSession.isOpen()).thenReturn(true);
    Mockito.when(websocketSession.getAsyncRemote()).thenReturn(asyncRemote);
    Mockito.when(transport.getAsyncSendTimeoutMilliseconds()).thenReturn(5_000L);
    Mockito.when(transport.getProtocol()).thenReturn(wsProtocol);
    Mockito.when(asyncRemote.sendText(Mockito.anyString())).thenReturn(sendFuture);

    openEndpoint();

    Packet<OrthodoxValue> packet = deliveryPacket("/async");

    endpoint.deliver(packet);

    Mockito.verify(asyncRemote).sendText(Mockito.anyString());
    Mockito.verify(sendFuture).get(5_000L, TimeUnit.MILLISECONDS);
    Mockito.verify(wsProtocol).onDelivery(packet);
  }
}
