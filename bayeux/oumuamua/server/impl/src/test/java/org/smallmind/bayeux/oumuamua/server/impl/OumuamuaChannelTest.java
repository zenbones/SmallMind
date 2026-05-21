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

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OumuamuaChannelTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;
  private ChannelRoot<OrthodoxValue> root;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);
    Mockito.when(server.getCodec()).thenReturn(codec);

    root = new ChannelRoot<>(server);
  }

  private OumuamuaChannel<OrthodoxValue> channel (String path)
    throws Exception {

    return channel(path, 60_000L, (c, s) -> {
    }, (c, s) -> {
    });
  }

  private OumuamuaChannel<OrthodoxValue> channel (String path, long ttl, java.util.function.BiConsumer<Channel<OrthodoxValue>, Session<OrthodoxValue>> sub, java.util.function.BiConsumer<Channel<OrthodoxValue>, Session<OrthodoxValue>> unsub)
    throws Exception {

    return new OumuamuaChannel<>(sub, unsub, ttl, new DefaultRoute(path), root);
  }

  @SuppressWarnings("unchecked")
  private Session<OrthodoxValue> mockSession (String id) {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(session.getId()).thenReturn(id);

    return session;
  }

  public void testRouteAccessor ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo/bar");

    Assert.assertEquals(c.getRoute().getPath(), "/foo/bar");
  }

  public void testPersistentFlagToggle ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");

    Assert.assertFalse(c.isPersistent());
    c.setPersistent(true);
    Assert.assertTrue(c.isPersistent());
    c.setPersistent(false);
    Assert.assertFalse(c.isPersistent());
  }

  public void testReflectingAndStreamingFlagsHonorRootAtCreation ()
    throws Exception {

    Mockito.when(server.isReflecting(Mockito.any())).thenReturn(true);
    Mockito.when(server.isStreaming(Mockito.any())).thenReturn(true);

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");

    Assert.assertTrue(c.isReflecting());
    Assert.assertTrue(c.isStreaming());

    c.setReflecting(false);
    c.setStreaming(false);
    Assert.assertFalse(c.isReflecting());
    Assert.assertFalse(c.isStreaming());
  }

  public void testSubscribeFiresCallbackOnce ()
    throws Exception {

    AtomicInteger calls = new AtomicInteger();
    AtomicReference<Session<OrthodoxValue>> last = new AtomicReference<>();

    OumuamuaChannel<OrthodoxValue> c = channel("/foo", 60_000L, (ch, s) -> {
      calls.incrementAndGet();
      last.set(s);
    }, (ch, s) -> {
    });

    Session<OrthodoxValue> session = mockSession("alice");

    Assert.assertTrue(c.subscribe(session));
    Assert.assertTrue(c.subscribe(session));
    Assert.assertEquals(calls.get(), 1);
    Assert.assertSame(last.get(), session);
  }

  public void testUnsubscribeFiresCallback ()
    throws Exception {

    AtomicInteger calls = new AtomicInteger();

    OumuamuaChannel<OrthodoxValue> c = channel("/foo", 60_000L, (ch, s) -> {
    }, (ch, s) -> calls.incrementAndGet());

    Session<OrthodoxValue> session = mockSession("alice");

    c.subscribe(session);
    c.unsubscribe(session);
    Assert.assertEquals(calls.get(), 1);
    c.unsubscribe(session);
    Assert.assertEquals(calls.get(), 1);
  }

  public void testIsRemovableTrueWhenIdleAndExpired ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo", 0L, (ch, s) -> {
    }, (ch, s) -> {
    });

    Thread.sleep(2L);
    Assert.assertTrue(c.isRemovable(System.currentTimeMillis()));
  }

  public void testIsRemovableFalseWhilePersistent ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo", 0L, (ch, s) -> {
    }, (ch, s) -> {
    });

    c.setPersistent(true);
    Thread.sleep(2L);
    Assert.assertFalse(c.isRemovable(System.currentTimeMillis() + 60_000L));
  }

  public void testIsRemovableFalseWhileSubscribed ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo", 0L, (ch, s) -> {
    }, (ch, s) -> {
    });

    Session<OrthodoxValue> session = mockSession("alice");

    c.subscribe(session);
    Thread.sleep(2L);
    Assert.assertFalse(c.isRemovable(System.currentTimeMillis() + 60_000L));
  }

  public void testIsRemovableFalseWithPersistentListener ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo", 0L, (ch, s) -> {
    }, (ch, s) -> {
    });

    Channel.SessionListener<OrthodoxValue> persistentListener = new Channel.SessionListener<>() {

      @Override
      public boolean isPersistent () {

        return true;
      }

      @Override
      public void onSubscribed (Session<OrthodoxValue> session) {

      }

      @Override
      public void onUnsubscribed (Session<OrthodoxValue> session) {

      }
    };

    c.addListener(persistentListener);
    Thread.sleep(2L);
    Assert.assertFalse(c.isRemovable(System.currentTimeMillis() + 60_000L));

    c.removeListener(persistentListener);
    Thread.sleep(2L);
    Assert.assertTrue(c.isRemovable(System.currentTimeMillis() + 60_000L));
  }

  public void testTerminateBlocksFurtherSubscribesAndReturnsSubscribers ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> a = mockSession("a");
    Session<OrthodoxValue> b = mockSession("b");

    c.subscribe(a);
    c.subscribe(b);

    org.smallmind.nutsnbolts.util.Pair<OumuamuaChannel<OrthodoxValue>, java.util.Set<Session<OrthodoxValue>>> terminated = c.terminate();

    Assert.assertSame(terminated.first(), c);
    Assert.assertEquals(terminated.second().size(), 2);
    Assert.assertTrue(terminated.second().contains(a));
    Assert.assertTrue(terminated.second().contains(b));
    Assert.assertFalse(c.subscribe(mockSession("late")));
  }

  public void testDeliverFansOutToSubscribers ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> a = mockSession("a");
    Session<OrthodoxValue> b = mockSession("b");

    c.subscribe(a);
    c.subscribe(b);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, c.getRoute(), message);

    c.deliver(null, packet, new HashSet<>());

    Mockito.verify(a).deliver(Mockito.eq(c), Mockito.isNull(), Mockito.any());
    Mockito.verify(b).deliver(Mockito.eq(c), Mockito.isNull(), Mockito.any());
  }

  public void testDeliverDedupesViaSessionIdSet ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> a = mockSession("a");

    c.subscribe(a);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, c.getRoute(), message);

    HashSet<String> seen = new HashSet<>();

    seen.add("a");
    c.deliver(null, packet, seen);
    Mockito.verify(a, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testDeliverSkipsSenderUnlessReflecting ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> sender = mockSession("alice");
    Session<OrthodoxValue> other = mockSession("bob");

    c.subscribe(sender);
    c.subscribe(other);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", c.getRoute(), message);

    c.deliver(sender, packet, new HashSet<>());

    Mockito.verify(sender, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(other).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testDeliverEchoesToSenderWhenReflecting ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");

    c.setReflecting(true);

    Session<OrthodoxValue> sender = mockSession("alice");

    c.subscribe(sender);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", c.getRoute(), message);

    c.deliver(sender, packet, new HashSet<>());

    Mockito.verify(sender).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testPacketListenerCanVeto ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> session = mockSession("a");

    c.subscribe(session);
    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }
    });

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, c.getRoute(), message);

    c.deliver(null, packet, new HashSet<>());

    Mockito.verify(session, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testPacketListenerCanReplacePacket ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> session = mockSession("a");

    c.subscribe(session);

    Message<OrthodoxValue> replacement = codec.create();

    replacement.put(Message.CHANNEL, "/foo");
    replacement.put("marker", "replaced");

    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return new Packet<>(packet.getPacketType(), packet.getSenderId(), packet.getRoute(), replacement);
      }
    });

    Message<OrthodoxValue> original = codec.create();

    original.put(Message.CHANNEL, "/foo");
    original.put("marker", "original");

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, c.getRoute(), original);
    org.mockito.ArgumentCaptor<Packet<OrthodoxValue>> captor = org.mockito.ArgumentCaptor.forClass(Packet.class);

    c.deliver(null, packet, new HashSet<>());

    Mockito.verify(session).deliver(Mockito.eq(c), Mockito.isNull(), captor.capture());

    Packet<OrthodoxValue> delivered = captor.getValue();

    Assert.assertEquals(delivered.getMessages().length, 1);
    Assert.assertEquals(((org.smallmind.bayeux.oumuamua.server.api.json.StringValue<OrthodoxValue>)delivered.getMessages()[0].get("marker")).asText(), "replaced");
  }

  public void testPacketListenerChainsReplacements ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> session = mockSession("a");

    c.subscribe(session);

    java.util.concurrent.atomic.AtomicReference<String> firstSawMarker = new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<String> secondSawMarker = new java.util.concurrent.atomic.AtomicReference<>();

    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        firstSawMarker.set(((org.smallmind.bayeux.oumuamua.server.api.json.StringValue<OrthodoxValue>)packet.getMessages()[0].get("marker")).asText());

        Message<OrthodoxValue> stage1 = codec.create();

        stage1.put(Message.CHANNEL, "/foo");
        stage1.put("marker", "stage1");

        return new Packet<>(packet.getPacketType(), packet.getSenderId(), packet.getRoute(), stage1);
      }
    });
    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        secondSawMarker.set(((org.smallmind.bayeux.oumuamua.server.api.json.StringValue<OrthodoxValue>)packet.getMessages()[0].get("marker")).asText());

        return packet;
      }
    });

    Message<OrthodoxValue> original = codec.create();

    original.put(Message.CHANNEL, "/foo");
    original.put("marker", "original");

    c.deliver(null, new Packet<>(PacketType.DELIVERY, null, c.getRoute(), original), new HashSet<>());

    Assert.assertEquals(firstSawMarker.get(), "original");
    Assert.assertEquals(secondSawMarker.get(), "stage1");
  }

  public void testPacketListenerSkippedForNonDelivery ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/foo");
    Session<OrthodoxValue> session = mockSession("a");
    AtomicInteger listenerCalls = new AtomicInteger();

    c.subscribe(session);
    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        listenerCalls.incrementAndGet();

        return packet;
      }
    });

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.RESPONSE, null, c.getRoute(), message);

    c.deliver(null, packet, new HashSet<>());

    Assert.assertEquals(listenerCalls.get(), 0);
  }
}
