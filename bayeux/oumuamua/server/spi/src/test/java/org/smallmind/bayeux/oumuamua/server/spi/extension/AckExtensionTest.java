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
package org.smallmind.bayeux.oumuamua.server.spi.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AckExtensionTest {

  private static final String ACK_FLAG = "org.smallmind.bayeux.oumuamua.extension.ack.flag";
  private static final String ACK_COUNTER = "org.smallmind.bayeux.oumuamua.extension.ack.counter";
  private static final String ACK_SIZE = "org.smallmind.bayeux.oumuamua.extension.ack.size";
  private static final String ACK_UNACKED_MAP = "org.smallmind.bayeux.oumuamua.extension.ack.unacknowledged_map";
  private static final String ACK_RESEND_QUEUE = "org.smallmind.bayeux.oumuamua.extension.ack.resend_queue";

  private OrthodoxValueFactory factory;
  private Session<OrthodoxValue> session;
  private Map<String, Object> attrs;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    factory = new OrthodoxValueFactory();
    session = Mockito.mock(Session.class);
    attrs = new HashMap<>();

    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.getAttribute(Mockito.anyString())).thenAnswer(inv -> attrs.get(inv.<String>getArgument(0)));
    Mockito.doAnswer(inv -> {
      attrs.put(inv.getArgument(0), inv.getArgument(1));

      return null;
    }).when(session).setAttribute(Mockito.anyString(), Mockito.any());
  }

  private Message<OrthodoxValue> message () {

    return new OrthodoxMessage(null, factory);
  }

  private Packet<OrthodoxValue> singlePacket (DefaultRoute route, Message<OrthodoxValue> message) {

    return new Packet<>(PacketType.REQUEST, "alice", route, message);
  }

  public void testHandshakeRequestWithAckTrueInitializesState () {

    Message<OrthodoxValue> handshake = message();

    handshake.getExt(true).put("ack", true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    Packet<OrthodoxValue> result = ext.onRequest(session, singlePacket(DefaultRoute.HANDSHAKE_ROUTE, handshake));

    Assert.assertSame(result.getMessages()[0], handshake);
    Assert.assertEquals(attrs.get(ACK_FLAG), Boolean.TRUE);
    Assert.assertNotNull(attrs.get(ACK_COUNTER));
    Assert.assertNotNull(attrs.get(ACK_SIZE));
    Assert.assertNotNull(attrs.get(ACK_UNACKED_MAP));
    Assert.assertNotNull(attrs.get(ACK_RESEND_QUEUE));
  }

  public void testHandshakeRequestWithoutAckIsIgnored () {

    Message<OrthodoxValue> handshake = message();

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, singlePacket(DefaultRoute.HANDSHAKE_ROUTE, handshake));

    Assert.assertNull(attrs.get(ACK_FLAG));
  }

  public void testHandshakeRequestWithAckFalseIsIgnored () {

    Message<OrthodoxValue> handshake = message();

    handshake.getExt(true).put("ack", false);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, singlePacket(DefaultRoute.HANDSHAKE_ROUTE, handshake));

    Assert.assertNull(attrs.get(ACK_FLAG));
  }

  public void testHandshakeResponseStampsAckAndSetsLongPolling () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> response = message();

    response.put(Message.SUCCESSFUL, true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);
    Packet<OrthodoxValue> output = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, response);

    ext.onResponse(session, output);
    Mockito.verify(session).setLongPolling(true);
    Assert.assertEquals(((org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue<OrthodoxValue>)response.getExt().get("ack")).asBoolean(), true);
  }

  public void testConnectResponseStampsAckIdAndStoresPacket () {

    ConcurrentSkipListMap<Long, Packet<OrthodoxValue>> unacked = new ConcurrentSkipListMap<>();

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, unacked);
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> connect = message();

    connect.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
    connect.put(Message.SUCCESSFUL, true);

    Message<OrthodoxValue> delivery = message();

    delivery.put(Message.CHANNEL, "/foo");

    Packet<OrthodoxValue> output = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, new Message[] {connect, delivery});

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);
    Packet<OrthodoxValue> result = ext.onResponse(session, output);

    NumberValue<OrthodoxValue> ackValue = (NumberValue<OrthodoxValue>)result.getMessages()[0].getExt().get("ack");

    Assert.assertEquals(ackValue.asLong(), 1L);
    Assert.assertEquals(unacked.size(), 1);
    Assert.assertNotNull(unacked.get(1L));
  }

  public void testConnectResponseSkipsWhenSingleMessage () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> connect = message();

    connect.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
    connect.put(Message.SUCCESSFUL, true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);
    Packet<OrthodoxValue> output = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, connect);

    ext.onResponse(session, output);
    Assert.assertNull(connect.getExt());
  }

  public void testConnectRequestAdvancesAckedAndQueuesLowerForResend () {

    ConcurrentSkipListMap<Long, Packet<OrthodoxValue>> unacked = new ConcurrentSkipListMap<>();
    ConcurrentLinkedQueue<Packet<OrthodoxValue>> resend = new ConcurrentLinkedQueue<>();
    AtomicLong size = new AtomicLong(3);

    Message<OrthodoxValue> m1 = message();
    Message<OrthodoxValue> m2 = message();
    Message<OrthodoxValue> m3 = message();

    Packet<OrthodoxValue> p1 = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, m1);
    Packet<OrthodoxValue> p2 = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, m2);
    Packet<OrthodoxValue> p3 = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, m3);

    unacked.put(1L, p1);
    unacked.put(2L, p2);
    unacked.put(3L, p3);

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(3));
    attrs.put(ACK_SIZE, size);
    attrs.put(ACK_UNACKED_MAP, unacked);
    attrs.put(ACK_RESEND_QUEUE, resend);

    Message<OrthodoxValue> connect = message();

    connect.getExt(true).put("ack", 2L);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, connect);

    ext.onRequest(session, packet);
    Assert.assertFalse(unacked.containsKey(1L));
    Assert.assertFalse(unacked.containsKey(2L));
    Assert.assertTrue(unacked.containsKey(3L));
    Assert.assertEquals(resend.size(), 1);
    Assert.assertSame(resend.peek(), p1);
    Assert.assertEquals(size.get(), 1L);
  }

  public void testConnectRequestWithoutFlagIsIgnored () {

    Message<OrthodoxValue> connect = message();

    connect.getExt(true).put("ack", 99L);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, connect));

    Assert.assertNull(attrs.get(ACK_UNACKED_MAP));
  }

  public void testConnectResponseOverflowTrimsOldestEntries () {

    ConcurrentSkipListMap<Long, Packet<OrthodoxValue>> unacked = new ConcurrentSkipListMap<>();

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, unacked);
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    AckExtension<OrthodoxValue> ext = new AckExtension<>(3);

    for (int call = 0; call < 3; call++) {

      Message<OrthodoxValue> connect = message();

      connect.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
      connect.put(Message.SUCCESSFUL, true);

      Message<OrthodoxValue> delivery = message();

      delivery.put(Message.CHANNEL, "/foo");

      ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, new Message[] {connect, delivery}));
    }

    // Each call adds a 2-message packet to the map; with max=3, after the first call the size is 2 (no trim),
    // and each subsequent call pushes the size to 4, triggering a trim that polls the oldest entry until size <= 3.
    Assert.assertEquals(unacked.size(), 1);
    Assert.assertTrue(unacked.containsKey(3L));
    Assert.assertEquals(((AtomicLong)attrs.get(ACK_SIZE)).get(), 2L);
  }

  public void testHandshakeRequestInitializesOnceForMultipleAckMessages () {

    Message<OrthodoxValue> first = message();
    Message<OrthodoxValue> second = message();

    first.getExt(true).put("ack", true);
    second.getExt(true).put("ack", true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, new Message[] {first, second}));

    Assert.assertEquals(attrs.get(ACK_FLAG), Boolean.TRUE);
    Assert.assertEquals(((AtomicLong)attrs.get(ACK_COUNTER)).get(), 0L);
  }

  public void testHandshakeRequestFindsAckInLaterMessage () {

    Message<OrthodoxValue> first = message();
    Message<OrthodoxValue> second = message();

    second.getExt(true).put("ack", true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, new Message[] {first, second}));

    Assert.assertEquals(attrs.get(ACK_FLAG), Boolean.TRUE);
  }

  public void testConnectRequestUsesHighestAckIdAcrossMessages () {

    ConcurrentSkipListMap<Long, Packet<OrthodoxValue>> unacked = new ConcurrentSkipListMap<>();
    ConcurrentLinkedQueue<Packet<OrthodoxValue>> resend = new ConcurrentLinkedQueue<>();

    Message<OrthodoxValue> placeholder = message();

    unacked.put(1L, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, placeholder));
    unacked.put(2L, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, placeholder));
    unacked.put(3L, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, placeholder));

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(3));
    attrs.put(ACK_SIZE, new AtomicLong(3));
    attrs.put(ACK_UNACKED_MAP, unacked);
    attrs.put(ACK_RESEND_QUEUE, resend);

    Message<OrthodoxValue> lowerMessage = message();
    Message<OrthodoxValue> higherMessage = message();

    lowerMessage.getExt(true).put("ack", 1L);
    higherMessage.getExt(true).put("ack", 3L);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, new Message[] {lowerMessage, higherMessage}));

    // ackId=3 confirms everything: id=3 removed outright, ids 1 and 2 moved to resend queue.
    Assert.assertTrue(unacked.isEmpty());
    Assert.assertEquals(resend.size(), 2);
  }

  public void testOnRequestWithNullSenderReturnsPacketUnchanged () {

    Message<OrthodoxValue> handshake = message();

    handshake.getExt(true).put("ack", true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, handshake);
    Packet<OrthodoxValue> result = ext.onRequest(null, packet);

    Assert.assertSame(result, packet);
    Assert.assertTrue(attrs.isEmpty());
  }

  public void testOnResponseWithNullSenderReturnsPacketUnchanged () {

    Message<OrthodoxValue> connect = message();

    connect.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
    connect.put(Message.SUCCESSFUL, true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, connect);
    Packet<OrthodoxValue> result = ext.onResponse(null, packet);

    Assert.assertSame(result, packet);
  }

  public void testConnectResponseDrainsResendQueueAndStampsAck () {

    ConcurrentSkipListMap<Long, Packet<OrthodoxValue>> unacked = new ConcurrentSkipListMap<>();
    ConcurrentLinkedQueue<Packet<OrthodoxValue>> resend = new ConcurrentLinkedQueue<>();

    Message<OrthodoxValue> resendDelivery = message();

    resendDelivery.put(Message.CHANNEL, "/foo");
    resend.add(new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, resendDelivery));

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, unacked);
    attrs.put(ACK_RESEND_QUEUE, resend);

    Message<OrthodoxValue> connect = message();

    connect.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
    connect.put(Message.SUCCESSFUL, true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);
    Packet<OrthodoxValue> output = new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, connect);
    Packet<OrthodoxValue> result = ext.onResponse(session, output);

    Assert.assertTrue(resend.isEmpty());
    Assert.assertTrue(result.getMessages().length > 1);
    Assert.assertFalse(unacked.isEmpty());
  }

  public void testHandshakeRequestDoesNotReinitializeAlreadyActiveSession () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(5));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> handshake = message();

    handshake.getExt(true).put("ack", true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, singlePacket(DefaultRoute.HANDSHAKE_ROUTE, handshake));

    Assert.assertEquals(((AtomicLong)attrs.get(ACK_COUNTER)).get(), 5L);
  }

  public void testConstructorAcceptsNullOverflowLogLevel () {

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10, null);

    Assert.assertNotNull(ext, "Null overflow log level must be tolerated and translated to Level.OFF");
  }

  public void testHandshakeRequestWithExtButNoAckEntryIsIgnored () {

    Message<OrthodoxValue> handshake = message();

    handshake.getExt(true).put("other", "marker");

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, singlePacket(DefaultRoute.HANDSHAKE_ROUTE, handshake));

    Assert.assertNull(attrs.get(ACK_FLAG), "An ext object without an 'ack' entry must not initialize the session");
  }

  public void testHandshakeRequestWithNonBooleanAckIsIgnored () {

    Message<OrthodoxValue> handshake = message();

    handshake.getExt(true).put("ack", 42L);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, singlePacket(DefaultRoute.HANDSHAKE_ROUTE, handshake));

    Assert.assertNull(attrs.get(ACK_FLAG), "A numeric 'ack' on handshake must not initialize the session");
  }

  public void testConnectRequestWithMessageMissingExtIsIgnored () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> connect = message();

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, connect));
  }

  public void testConnectRequestWithNonNumericAckIsIgnored () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> connect = message();

    connect.getExt(true).put("ack", "not-a-number");

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, connect));
  }

  public void testConnectRequestExistingAckIdIgnoresLowerValueInLaterMessage () {

    ConcurrentSkipListMap<Long, Packet<OrthodoxValue>> unacked = new ConcurrentSkipListMap<>();

    unacked.put(1L, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, message()));
    unacked.put(2L, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, message()));
    unacked.put(3L, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, message()));

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(3));
    attrs.put(ACK_SIZE, new AtomicLong(3));
    attrs.put(ACK_UNACKED_MAP, unacked);
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> higher = message();
    Message<OrthodoxValue> lower = message();

    higher.getExt(true).put("ack", 3L);
    lower.getExt(true).put("ack", 1L);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, new Message[] {higher, lower}));

    Assert.assertTrue(unacked.isEmpty(), "The highest ack id encountered first must still be applied; later lower ids cannot rewind it");
  }

  public void testConnectRequestWithoutAnyAckIdLeavesQueueUnchanged () {

    ConcurrentSkipListMap<Long, Packet<OrthodoxValue>> unacked = new ConcurrentSkipListMap<>();

    unacked.put(1L, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, message()));

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(1));
    attrs.put(ACK_SIZE, new AtomicLong(1));
    attrs.put(ACK_UNACKED_MAP, unacked);
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> connect = message();

    connect.getExt(true).put("other", "value");

    AckExtension<OrthodoxValue> ext = new AckExtension<>(100);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, connect));

    Assert.assertEquals(unacked.size(), 1, "Connect request without an ack id must leave the unacknowledged map untouched");
  }

  public void testHandshakeResponseSkipsUnsuccessfulMessage () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> response = message();

    response.put(Message.SUCCESSFUL, false);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, response));

    Mockito.verify(session).setLongPolling(true);
    Assert.assertNull(response.getExt(), "Unsuccessful handshake response must not be stamped with ack=true");
  }

  public void testConnectResponseAckFlagNotSetSkipsAck () {

    Message<OrthodoxValue> connect = message();

    connect.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
    connect.put(Message.SUCCESSFUL, true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, connect));

    Assert.assertNull(connect.getExt(), "Without ACK_FLAG, connect responses must not be stamped");
  }

  public void testConnectResponseSkipsUnsuccessfulConnectMessage () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> connect = message();

    connect.put(Message.CHANNEL, DefaultRoute.CONNECT_ROUTE.getPath());
    connect.put(Message.SUCCESSFUL, false);

    Message<OrthodoxValue> delivery = message();

    delivery.put(Message.CHANNEL, "/foo");

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    Packet<OrthodoxValue> result = ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, new Message[] {connect, delivery}));

    Assert.assertNull(connect.getExt(), "Unsuccessful connect message must not receive an ack id");
    Assert.assertSame(result.getMessages()[0], connect, "Packet must remain unchanged when no connect message qualifies");
  }

  public void testConnectResponseWithNonConnectChannelSkipsAck () {

    attrs.put(ACK_FLAG, Boolean.TRUE);
    attrs.put(ACK_COUNTER, new AtomicLong(0));
    attrs.put(ACK_SIZE, new AtomicLong(0));
    attrs.put(ACK_UNACKED_MAP, new ConcurrentSkipListMap<Long, Packet<OrthodoxValue>>());
    attrs.put(ACK_RESEND_QUEUE, new ConcurrentLinkedQueue<Packet<OrthodoxValue>>());

    Message<OrthodoxValue> first = message();
    Message<OrthodoxValue> second = message();

    first.put(Message.CHANNEL, "/foo");
    first.put(Message.SUCCESSFUL, true);
    second.put(Message.CHANNEL, "/bar");
    second.put(Message.SUCCESSFUL, true);

    AckExtension<OrthodoxValue> ext = new AckExtension<>(10);

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, new Message[] {first, second}));

    Assert.assertNull(first.getExt(), "No connect-channel message means no ack id is stamped");
    Assert.assertNull(second.getExt());
  }
}
