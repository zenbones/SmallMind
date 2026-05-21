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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.websocket.Endpoint;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class WebsocketProtocolTest {

  private static final class StubEndpoint extends Endpoint {

    @Override
    public void onOpen (jakarta.websocket.Session session, jakarta.websocket.EndpointConfig config) {

    }
  }

  private static final class CountingProtocolListener implements Protocol.ProtocolListener<OrthodoxValue> {

    private final AtomicInteger receiptCount = new AtomicInteger();
    private final AtomicInteger publishCount = new AtomicInteger();
    private final AtomicInteger deliveryCount = new AtomicInteger();

    @Override
    public void onReceipt (Message<OrthodoxValue>[] incomingMessages) {

      receiptCount.incrementAndGet();
    }

    @Override
    public void onPublish (Message<OrthodoxValue> originatingMessage, Message<OrthodoxValue> outgoingMessage) {

      publishCount.incrementAndGet();
    }

    @Override
    public void onDelivery (Packet<OrthodoxValue> outgoingPacket) {

      deliveryCount.incrementAndGet();
    }

    int receipts () {

      return receiptCount.get();
    }

    int publishes () {

      return publishCount.get();
    }

    int deliveries () {

      return deliveryCount.get();
    }
  }

  private WebsocketProtocol<OrthodoxValue> protocol;

  @BeforeMethod
  public void beforeMethod () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/cometd");

    protocol = new WebsocketProtocol<>(20000L, config);
  }

  public void testGetNameReturnsWebsocket () {

    Assert.assertEquals(protocol.getName(), "websocket");
  }

  public void testIsLongPollingReturnsFalse () {

    Assert.assertFalse(protocol.isLongPolling());
  }

  public void testGetLongPollTimeoutReturnsConfiguredValue () {

    Assert.assertEquals(protocol.getLongPollTimeoutMilliseconds(), 20000L);
  }

  public void testGetTransportNamesReturnsSingleWebsocketEntry () {

    String[] names = protocol.getTransportNames();

    Assert.assertNotNull(names);
    Assert.assertEquals(names.length, 1);
    Assert.assertEquals(names[0], Transports.WEBSOCKET.getName());
  }

  public void testGetTransportReturnsWebSocketTransportForMatchingName () {

    Assert.assertNotNull(protocol.getTransport(Transports.WEBSOCKET.getName()));
  }

  public void testGetTransportReturnsNullForUnknownName () {

    Assert.assertNull(protocol.getTransport("long-polling"));
    Assert.assertNull(protocol.getTransport("unknown"));
  }

  public void testGetTransportOwnerIsThisProtocol () {

    Assert.assertSame(protocol.getTransport(Transports.WEBSOCKET.getName()).getProtocol(), protocol);
  }

  public void testConstructorWithNullListenersSucceeds () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");

    new WebsocketProtocol<OrthodoxValue>(5000L, config, null);
  }

  @SuppressWarnings("unchecked")
  public void testConstructorWithListenersRegistersEach () {

    WebsocketConfiguration config = new WebsocketConfiguration(StubEndpoint.class, "/ws");
    CountingProtocolListener listenerA = new CountingProtocolListener();
    CountingProtocolListener listenerB = new CountingProtocolListener();

    WebsocketProtocol<OrthodoxValue> protocolWithListeners = new WebsocketProtocol<>(
      5000L, config, new Protocol.ProtocolListener[] {listenerA, listenerB});

    protocolWithListeners.onReceipt(new Message[0]);

    Assert.assertEquals(listenerA.receipts(), 1);
    Assert.assertEquals(listenerB.receipts(), 1);
  }

  @SuppressWarnings("unchecked")
  public void testOnReceiptFansOutToAllRegisteredListeners () {

    CountingProtocolListener listenerA = new CountingProtocolListener();
    CountingProtocolListener listenerB = new CountingProtocolListener();

    protocol.addListener(listenerA);
    protocol.addListener(listenerB);

    protocol.onReceipt(new Message[0]);
    protocol.onReceipt(new Message[0]);

    Assert.assertEquals(listenerA.receipts(), 2);
    Assert.assertEquals(listenerB.receipts(), 2);
    Assert.assertEquals(listenerA.publishes(), 0);
    Assert.assertEquals(listenerA.deliveries(), 0);
  }

  @SuppressWarnings("unchecked")
  public void testOnPublishFansOutToAllRegisteredListeners () {

    CountingProtocolListener listener = new CountingProtocolListener();
    Message<OrthodoxValue> originating = Mockito.mock(Message.class);
    Message<OrthodoxValue> outgoing = Mockito.mock(Message.class);

    protocol.addListener(listener);
    protocol.onPublish(originating, outgoing);

    Assert.assertEquals(listener.publishes(), 1);
    Assert.assertEquals(listener.receipts(), 0);
    Assert.assertEquals(listener.deliveries(), 0);
  }

  @SuppressWarnings("unchecked")
  public void testOnDeliveryFansOutToAllRegisteredListeners () {

    CountingProtocolListener listener = new CountingProtocolListener();
    Route route = Mockito.mock(Route.class);
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, route, new Message[0]);

    protocol.addListener(listener);
    protocol.onDelivery(packet);

    Assert.assertEquals(listener.deliveries(), 1);
    Assert.assertEquals(listener.receipts(), 0);
    Assert.assertEquals(listener.publishes(), 0);
  }

  @SuppressWarnings("unchecked")
  public void testRemoveListenerStopsFanout () {

    CountingProtocolListener listener = new CountingProtocolListener();

    protocol.addListener(listener);
    protocol.onReceipt(new Message[0]);
    Assert.assertEquals(listener.receipts(), 1);

    protocol.removeListener(listener);
    protocol.onReceipt(new Message[0]);
    Assert.assertEquals(listener.receipts(), 1);
  }

  @SuppressWarnings("unchecked")
  public void testRemoveUnregisteredListenerIsNoOp () {

    CountingProtocolListener never = new CountingProtocolListener();

    protocol.removeListener(never);
    protocol.onReceipt(new Message[0]);

    Assert.assertEquals(never.receipts(), 0);
  }
}
