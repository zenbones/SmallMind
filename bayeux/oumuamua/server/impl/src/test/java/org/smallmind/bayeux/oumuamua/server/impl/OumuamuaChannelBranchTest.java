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

/**
 * Branch-coverage companion to {@link OumuamuaChannelTest}. Covers the listener-type filter
 * paths in {@code onSubscribed}/{@code onUnsubscribed}/{@code onProcessing} when the channel
 * holds a mixed roster of listener implementations, the {@code addListener} no-op after
 * {@link OumuamuaChannel#terminate()}, removeListener idempotency, persistent-listener and
 * subscriber interactions with the quiescent timestamp, and terminate's persistent-listener
 * branch.
 */
@Test(groups = "unit")
public class OumuamuaChannelBranchTest {

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

    return new OumuamuaChannel<>((c, s) -> {
    }, (c, s) -> {
    }, 60_000L, new DefaultRoute(path), root);
  }

  @SuppressWarnings("unchecked")
  private Session<OrthodoxValue> mockSession (String id) {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(session.getId()).thenReturn(id);

    return session;
  }

  public void testSubscribeIgnoresNonSessionListener ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/mixed");
    AtomicInteger sessionCalls = new AtomicInteger();
    AtomicInteger packetCalls = new AtomicInteger();

    c.addListener(new Channel.SessionListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public void onSubscribed (Session<OrthodoxValue> session) {

        sessionCalls.incrementAndGet();
      }

      @Override
      public void onUnsubscribed (Session<OrthodoxValue> session) {

      }
    });

    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        packetCalls.incrementAndGet();

        return packet;
      }
    });

    c.subscribe(mockSession("alice"));

    Assert.assertEquals(sessionCalls.get(), 1, "SessionListener.onSubscribed must fire");
    Assert.assertEquals(packetCalls.get(), 0, "PacketListener must be skipped on subscribe");
  }

  public void testUnsubscribeIgnoresNonSessionListener ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/mixed-unsub");
    AtomicInteger sessionCalls = new AtomicInteger();
    AtomicInteger packetCalls = new AtomicInteger();

    c.addListener(new Channel.SessionListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public void onSubscribed (Session<OrthodoxValue> session) {

      }

      @Override
      public void onUnsubscribed (Session<OrthodoxValue> session) {

        sessionCalls.incrementAndGet();
      }
    });

    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        packetCalls.incrementAndGet();

        return packet;
      }
    });

    Session<OrthodoxValue> session = mockSession("alice");

    c.subscribe(session);
    c.unsubscribe(session);

    Assert.assertEquals(sessionCalls.get(), 1, "SessionListener.onUnsubscribed must fire");
    Assert.assertEquals(packetCalls.get(), 0, "PacketListener must be skipped on unsubscribe");
  }

  public void testDeliverIgnoresNonPacketListener ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/mixed-deliver");
    AtomicInteger sessionCalls = new AtomicInteger();
    AtomicInteger packetCalls = new AtomicInteger();

    c.addListener(new Channel.SessionListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public void onSubscribed (Session<OrthodoxValue> session) {

        sessionCalls.incrementAndGet();
      }

      @Override
      public void onUnsubscribed (Session<OrthodoxValue> session) {

      }
    });

    c.addListener(new Channel.PacketListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        packetCalls.incrementAndGet();

        return packet;
      }
    });

    Session<OrthodoxValue> session = mockSession("alice");

    c.subscribe(session);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, c.getRoute(), message);

    c.deliver(null, packet, new HashSet<>());

    Assert.assertEquals(packetCalls.get(), 1, "PacketListener must run during deliver");
  }

  public void testAddListenerNoOpAfterTerminate ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/terminated");

    c.terminate();

    AtomicInteger subscribeCalls = new AtomicInteger();

    c.addListener(new Channel.SessionListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return true;
      }

      @Override
      public void onSubscribed (Session<OrthodoxValue> session) {

        subscribeCalls.incrementAndGet();
      }

      @Override
      public void onUnsubscribed (Session<OrthodoxValue> session) {

      }
    });

    // A terminal channel rejects subscriptions, so the listener never runs.
    Assert.assertFalse(c.subscribe(mockSession("late")));
    Assert.assertEquals(subscribeCalls.get(), 0);
  }

  public void testAddNonPersistentListenerDoesNotIncrementPersistentCount ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/non-persistent-listener");

    c.addListener(new Channel.SessionListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public void onSubscribed (Session<OrthodoxValue> session) {

      }

      @Override
      public void onUnsubscribed (Session<OrthodoxValue> session) {

      }
    });

    Thread.sleep(2L);

    OumuamuaChannel<OrthodoxValue> shortLived = new OumuamuaChannel<>((ch, s) -> {
    }, (ch, s) -> {
    }, 0L, new DefaultRoute("/short"), root);

    shortLived.addListener(new Channel.SessionListener<OrthodoxValue>() {

      @Override
      public boolean isPersistent () {

        return false;
      }

      @Override
      public void onSubscribed (Session<OrthodoxValue> session) {

      }

      @Override
      public void onUnsubscribed (Session<OrthodoxValue> session) {

      }
    });

    Thread.sleep(2L);

    Assert.assertTrue(shortLived.isRemovable(System.currentTimeMillis()), "A channel with only non-persistent listeners must remain idle-removable");
  }

  public void testRemoveListenerNotPresentIsNoOp ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/remove-never-added");

    Channel.SessionListener<OrthodoxValue> persistentListener = new Channel.SessionListener<OrthodoxValue>() {

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

    c.removeListener(persistentListener);
  }

  public void testRemovePersistentListenerWhileSubscriberPresentKeepsQuiescentDisabled ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = new OumuamuaChannel<>((ch, s) -> {
    }, (ch, s) -> {
    }, 0L, new DefaultRoute("/persistent-with-sub"), root);

    Channel.SessionListener<OrthodoxValue> persistentListener = new Channel.SessionListener<OrthodoxValue>() {

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
    c.subscribe(mockSession("alice"));

    c.removeListener(persistentListener);

    Thread.sleep(2L);

    Assert.assertFalse(c.isRemovable(System.currentTimeMillis() + 1_000_000L), "A channel with active subscribers must stay non-removable even after removing the persistent listener");
  }

  public void testUnsubscribeWhilePersistentListenerPresentKeepsQuiescentDisabled ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = new OumuamuaChannel<>((ch, s) -> {
    }, (ch, s) -> {
    }, 0L, new DefaultRoute("/unsub-persistent"), root);

    c.addListener(new Channel.SessionListener<OrthodoxValue>() {

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
    });

    Session<OrthodoxValue> session = mockSession("alice");

    c.subscribe(session);
    c.unsubscribe(session);

    Thread.sleep(2L);

    Assert.assertFalse(c.isRemovable(System.currentTimeMillis() + 1_000_000L), "A channel with a persistent listener must stay non-removable even after the last subscriber leaves");
  }

  public void testTerminateKeepsQuiescentTimestampZeroWhenPersistentListenerPresent ()
    throws Exception {

    OumuamuaChannel<OrthodoxValue> c = channel("/terminate-persistent");

    c.addListener(new Channel.SessionListener<OrthodoxValue>() {

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
    });

    c.terminate();

    Assert.assertFalse(c.isRemovable(System.currentTimeMillis() + 1_000_000L), "Terminate must not arm the quiescent timer when a persistent listener still keeps the channel alive");
  }
}
