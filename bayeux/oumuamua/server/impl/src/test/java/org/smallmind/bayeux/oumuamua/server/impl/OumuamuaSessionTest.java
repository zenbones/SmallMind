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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class OumuamuaSessionTest {

  private OrthodoxCodec codec;
  private Connection<OrthodoxValue> connection;
  private Transport<OrthodoxValue> transport;
  private Protocol<OrthodoxValue> protocol;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    connection = Mockito.mock(Connection.class);
    transport = Mockito.mock(Transport.class);
    protocol = Mockito.mock(Protocol.class);

    Mockito.when(connection.getTransport()).thenReturn(transport);
    Mockito.when(transport.getProtocol()).thenReturn(protocol);
    Mockito.when(protocol.isLongPolling()).thenReturn(false);
  }

  private OumuamuaSession<OrthodoxValue> session (java.util.function.Consumer<org.smallmind.bayeux.oumuamua.server.api.Session<OrthodoxValue>> connected, java.util.function.Consumer<org.smallmind.bayeux.oumuamua.server.api.Session<OrthodoxValue>> disconnected) {

    return new OumuamuaSession<>(connected, disconnected, connection, 4, 60_000L, Level.DEBUG);
  }

  private OumuamuaSession<OrthodoxValue> session () {

    return session(s -> {
    }, s -> {
    });
  }

  private Packet<OrthodoxValue> deliveryPacket (String channel) {

    Message<OrthodoxValue> message = codec.create();
    message.put(Message.CHANNEL, channel);

    try {

      return new Packet<>(PacketType.DELIVERY, null, new DefaultRoute(channel), message);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public void testInitialStateIsInitialized () {

    Assert.assertEquals(session().getState(), SessionState.INITIALIZED);
  }

  public void testCompleteHandshakeAdvancesState () {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeHandshake();
    Assert.assertEquals(s.getState(), SessionState.HANDSHOOK);
  }

  public void testCompleteConnectionAdvancesStateAndFiresCallback () {

    AtomicInteger fires = new AtomicInteger();
    OumuamuaSession<OrthodoxValue> s = session(x -> fires.incrementAndGet(), x -> {
    });

    s.completeConnection();
    Assert.assertEquals(s.getState(), SessionState.CONNECTED);
    Assert.assertEquals(fires.get(), 1);
  }

  public void testCompleteDisconnectFiresCallbackOnce () {

    AtomicInteger fires = new AtomicInteger();
    OumuamuaSession<OrthodoxValue> s = session(x -> {
    }, x -> fires.incrementAndGet());

    s.completeDisconnect();
    s.completeDisconnect();
    Assert.assertEquals(s.getState(), SessionState.DISCONNECTED);
    Assert.assertEquals(fires.get(), 1);
  }

  public void testLongPollingFromProtocol () {

    Mockito.when(protocol.isLongPolling()).thenReturn(true);
    Assert.assertTrue(session().isLongPolling());
  }

  public void testLongPollingOverride () {

    OumuamuaSession<OrthodoxValue> s = session();

    Assert.assertFalse(s.isLongPolling());
    s.setLongPolling(true);
    Assert.assertTrue(s.isLongPolling());
  }

  public void testIdAndAccessors () {

    OumuamuaSession<OrthodoxValue> s = session();

    Assert.assertNotNull(s.getId());
    Assert.assertFalse(s.getId().isEmpty());
    Assert.assertEquals(s.getMaxLongPollQueueSize(), 4);
    Assert.assertSame(s.getTransport(), transport);
  }

  public void testDispatchDelegatesToConnection () {

    OumuamuaSession<OrthodoxValue> s = session();
    Packet<OrthodoxValue> packet = deliveryPacket("/foo");

    s.dispatch(packet);
    Mockito.verify(connection).deliver(packet);
  }

  public void testDeliverIgnoredWhenNotConnected () {

    OumuamuaSession<OrthodoxValue> s = session();
    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    s.deliver(channel, null, deliveryPacket("/foo"));
    Mockito.verify(connection, Mockito.never()).deliver(Mockito.any());
  }

  public void testDeliverIgnoredWhenHandshookButNotConnected () {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeHandshake();

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(true);

    s.deliver(channel, null, deliveryPacket("/foo"));
    Mockito.verify(connection, Mockito.never()).deliver(Mockito.any());
  }

  public void testDeliverIgnoredWhenDisconnected () {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();
    s.completeDisconnect();

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(true);

    s.deliver(channel, null, deliveryPacket("/foo"));
    Mockito.verify(connection, Mockito.never()).deliver(Mockito.any());
  }

  public void testDeliverStreamingBypassesQueue () {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(true);

    Packet<OrthodoxValue> packet = deliveryPacket("/foo");

    s.deliver(channel, null, packet);
    Mockito.verify(connection).deliver(packet);
  }

  public void testDeliverNonStreamingDispatchesDirectlyWhenNotLongPolling () {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    Packet<OrthodoxValue> packet = deliveryPacket("/foo");

    s.deliver(channel, null, packet);
    Mockito.verify(connection).deliver(packet);
  }

  public void testDeliverLongPollingEnqueuesAndPollReturnsIt ()
    throws InterruptedException {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();
    s.setLongPolling(true);

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    Packet<OrthodoxValue> packet = deliveryPacket("/foo");

    s.deliver(channel, null, packet);

    Packet<OrthodoxValue> polled = s.poll(50L, TimeUnit.MILLISECONDS);

    Assert.assertSame(polled, packet);
    Mockito.verify(connection, Mockito.never()).deliver(Mockito.any());
  }

  public void testPollReturnsNullOnTimeout ()
    throws InterruptedException {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();
    Assert.assertNull(s.poll(50L, TimeUnit.MILLISECONDS));
  }

  public void testPollUnblocksOnDisconnect ()
    throws InterruptedException {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();

    Thread poller = new Thread(() -> {
      try {
        s.poll(10_000L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException ignored) {
      }
    });

    poller.start();
    Thread.sleep(20L);
    s.completeDisconnect();
    poller.join(2_000L);
    Assert.assertFalse(poller.isAlive());
  }

  public void testLongPollOverflowDropsOldest ()
    throws InterruptedException {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();
    s.setLongPolling(true);

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    Packet<OrthodoxValue> p1 = deliveryPacket("/foo/1");
    Packet<OrthodoxValue> p2 = deliveryPacket("/foo/2");
    Packet<OrthodoxValue> p3 = deliveryPacket("/foo/3");
    Packet<OrthodoxValue> p4 = deliveryPacket("/foo/4");
    Packet<OrthodoxValue> p5 = deliveryPacket("/foo/5");

    s.deliver(channel, null, p1);
    s.deliver(channel, null, p2);
    s.deliver(channel, null, p3);
    s.deliver(channel, null, p4);
    s.deliver(channel, null, p5);

    Assert.assertSame(s.poll(50L, TimeUnit.MILLISECONDS), p2);
  }

  public void testIsRemovableAfterIdleTimeout () {

    OumuamuaSession<OrthodoxValue> s = new OumuamuaSession<>(x -> {
    }, x -> {
    }, connection, 4, 0L, Level.DEBUG);

    try {
      Thread.sleep(2L);
    } catch (InterruptedException ignored) {
    }

    Assert.assertTrue(s.isRemovable(System.currentTimeMillis()));
  }

  public void testContactResetsIdleTimer ()
    throws InterruptedException {

    OumuamuaSession<OrthodoxValue> s = new OumuamuaSession<>(x -> {
    }, x -> {
    }, connection, 4, 100L, Level.DEBUG);

    Thread.sleep(2L);
    s.contact();
    Assert.assertFalse(s.isRemovable(System.currentTimeMillis()));
  }

  public void testCheckAndDisconnectTransitionsAtomically ()
    throws InterruptedException {

    AtomicInteger fires = new AtomicInteger();
    OumuamuaSession<OrthodoxValue> s = new OumuamuaSession<>(x -> {
    }, x -> fires.incrementAndGet(), connection, 4, 0L, Level.DEBUG);

    Thread.sleep(2L);
    Assert.assertTrue(s.checkAndDisconnect(System.currentTimeMillis()));
    Assert.assertEquals(s.getState(), SessionState.DISCONNECTED);
    Assert.assertEquals(fires.get(), 1);
    Assert.assertFalse(s.checkAndDisconnect(System.currentTimeMillis()));
  }

  public void testHijackReplacesConnection () {

    OumuamuaSession<OrthodoxValue> s = session();

    @SuppressWarnings("unchecked") Connection<OrthodoxValue> other = Mockito.mock(Connection.class);
    @SuppressWarnings("unchecked") Transport<OrthodoxValue> otherTransport = Mockito.mock(Transport.class);

    Mockito.when(other.getTransport()).thenReturn(otherTransport);

    s.hijack(other);
    Assert.assertSame(s.getTransport(), otherTransport);
  }

  public void testOnCleanupDelegatesToConnection () {

    session().onCleanup();
    Mockito.verify(connection).onCleanup();
  }

  public void testIsLocalDelegatesToTransport () {

    Mockito.when(transport.isLocal()).thenReturn(true);
    Assert.assertTrue(session().isLocal());
  }

  public void testAddListenerAndRemoveListenerControlPacketFiltering () {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(true);

    Session.PacketListener<OrthodoxValue> vetoer = new Session.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }
    };

    s.addListener(vetoer);
    s.deliver(channel, null, deliveryPacket("/vetoed"));

    Mockito.verify(connection, Mockito.never()).deliver(Mockito.any());

    s.removeListener(vetoer);
    s.deliver(channel, null, deliveryPacket("/allowed"));

    Mockito.verify(connection).deliver(Mockito.any());
  }

  public void testSessionPacketListenerCanVetoDeliveryWhenLongPolling ()
    throws InterruptedException {

    OumuamuaSession<OrthodoxValue> s = session();

    s.completeConnection();
    s.setLongPolling(true);

    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    s.addListener(new Session.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }
    });

    s.deliver(channel, null, deliveryPacket("/long-poll-vetoed"));

    Assert.assertNull(s.poll(50L, java.util.concurrent.TimeUnit.MILLISECONDS), "Vetoed delivery must not appear in the long-poll queue");
  }
}
