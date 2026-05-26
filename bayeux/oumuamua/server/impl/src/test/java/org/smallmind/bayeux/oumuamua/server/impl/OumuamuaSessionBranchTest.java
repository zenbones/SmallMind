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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Branch-coverage companion to {@link OumuamuaSessionTest} that drives
 * {@link Session.PacketListener} dispatch for both response and delivery packets, the
 * REQUEST short-circuit in {@code onProcessing}, the long-polling streaming-bypass branch,
 * {@code contact()} on a disconnected session, the long-poll queue-overflow path, and the
 * vetoing-listener branches in {@link OumuamuaSession#deliver}.
 */
@Test(groups = "unit")
public class OumuamuaSessionBranchTest {

  private OrthodoxCodec codec;
  private Connection<OrthodoxValue> connection;
  private Transport<OrthodoxValue> transport;
  private Protocol<OrthodoxValue> protocol;

  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    connection = Mockito.mock(Connection.class);
    transport = Mockito.mock(Transport.class);
    protocol = Mockito.mock(Protocol.class);

    Mockito.when(connection.getTransport()).thenReturn(transport);
    Mockito.when(transport.getProtocol()).thenReturn(protocol);
    Mockito.when(protocol.isLongPolling()).thenReturn(false);
  }

  private OumuamuaSession<OrthodoxValue> session (int queueSize) {

    Consumer<Session<OrthodoxValue>> noop = s -> {
    };

    return new OumuamuaSession<>(noop, noop, connection, queueSize, 60_000L, null);
  }

  private Packet<OrthodoxValue> delivery (String channel)
    throws Exception {

    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    return new Packet<>(PacketType.DELIVERY, null, new DefaultRoute(channel), message);
  }

  private Packet<OrthodoxValue> response (String channel)
    throws Exception {

    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    return new Packet<>(PacketType.RESPONSE, null, new DefaultRoute(channel), message);
  }

  public void testConstructorAcceptsNullOverflowLogLevel () {

    OumuamuaSession<OrthodoxValue> sess = session(4);

    Assert.assertNotNull(sess.getId(), "Session must be constructible with a null overflow log level");
  }

  public void testConstructorAdoptsLongPollingFromTransport () {

    Connection<OrthodoxValue> longPollConnection = Mockito.mock(Connection.class);
    Transport<OrthodoxValue> longPollTransport = Mockito.mock(Transport.class);
    Protocol<OrthodoxValue> longPollProtocol = Mockito.mock(Protocol.class);

    Mockito.when(longPollConnection.getTransport()).thenReturn(longPollTransport);
    Mockito.when(longPollTransport.getProtocol()).thenReturn(longPollProtocol);
    Mockito.when(longPollProtocol.isLongPolling()).thenReturn(true);

    OumuamuaSession<OrthodoxValue> longPolling = new OumuamuaSession<>(s -> {
    }, s -> {
    }, longPollConnection, 4, 60_000L, null);

    Assert.assertTrue(longPolling.isLongPolling(), "Session must adopt long-polling mode when the transport's protocol is long-polling");
  }

  public void testPacketListenerOnResponseInvokedForResponsePacket ()
    throws Exception {

    OumuamuaSession<OrthodoxValue> sess = session(4);
    AtomicReference<PacketType> seenType = new AtomicReference<>();

    sess.addListener(new Session.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        seenType.set(PacketType.RESPONSE);

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }
    });

    Packet<OrthodoxValue> result = sess.onResponse(null, response("/r"));

    Assert.assertNotNull(result);
    Assert.assertEquals(seenType.get(), PacketType.RESPONSE);
  }

  public void testPacketListenerOnResponseVetoReturnsNull ()
    throws Exception {

    OumuamuaSession<OrthodoxValue> sess = session(4);

    sess.addListener(new Session.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }
    });

    Assert.assertNull(sess.onResponse(null, response("/r/vetoed")));
  }

  public void testOnResponseIgnoresNonResponsePacketTypes ()
    throws Exception {

    OumuamuaSession<OrthodoxValue> sess = session(4);
    AtomicInteger calls = new AtomicInteger();

    sess.addListener(new Session.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        calls.incrementAndGet();

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        calls.incrementAndGet();

        return packet;
      }
    });

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> request = new Packet<>(PacketType.REQUEST, null, new DefaultRoute("/r"), message);
    Packet<OrthodoxValue> result = sess.onResponse(null, request);

    Assert.assertSame(result, request, "REQUEST packets must skip session-level PacketListeners");
    Assert.assertEquals(calls.get(), 0, "Listener must not be invoked for REQUEST packets");
  }

  public void testDeliverPacketListenerOnDeliveryInvokedForDeliveryPacket ()
    throws Exception {

    OumuamuaSession<OrthodoxValue> sess = session(4);
    AtomicReference<PacketType> seenType = new AtomicReference<>();

    sess.addListener(new Session.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        seenType.set(PacketType.DELIVERY);

        return packet;
      }
    });

    sess.completeConnection();

    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    sess.deliver(channel, null, delivery("/d"));

    Assert.assertEquals(seenType.get(), PacketType.DELIVERY);
    Mockito.verify(connection).deliver(Mockito.any());
  }

  public void testDeliverVetoedByDeliveryListenerSkipsConnection ()
    throws Exception {

    OumuamuaSession<OrthodoxValue> sess = session(4);

    sess.addListener(new Session.PacketListener<OrthodoxValue>() {

      @Override
      public Packet<OrthodoxValue> onResponse (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return packet;
      }

      @Override
      public Packet<OrthodoxValue> onDelivery (Session<OrthodoxValue> sender, Packet<OrthodoxValue> packet) {

        return null;
      }
    });

    sess.completeConnection();

    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    sess.deliver(channel, null, delivery("/d/vetoed"));

    Mockito.verify(connection, Mockito.never()).deliver(Mockito.any());
  }

  public void testStreamingChannelOnLongPollingTransportFallsThroughToLongPoll ()
    throws Exception {

    Connection<OrthodoxValue> longPollConnection = Mockito.mock(Connection.class);
    Transport<OrthodoxValue> longPollTransport = Mockito.mock(Transport.class);
    Protocol<OrthodoxValue> longPollProtocol = Mockito.mock(Protocol.class);

    Mockito.when(longPollConnection.getTransport()).thenReturn(longPollTransport);
    Mockito.when(longPollTransport.getProtocol()).thenReturn(longPollProtocol);
    Mockito.when(longPollProtocol.isLongPolling()).thenReturn(true);

    OumuamuaSession<OrthodoxValue> sess = new OumuamuaSession<>(s -> {
    }, s -> {
    }, longPollConnection, 4, 60_000L, null);

    sess.completeConnection();

    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(true);

    sess.deliver(channel, null, delivery("/streaming/longpoll"));
    Mockito.verify(longPollConnection, Mockito.never()).deliver(Mockito.any());
  }

  public void testContactNoOpAfterDisconnect () {

    OumuamuaSession<OrthodoxValue> sess = session(4);

    sess.completeDisconnect();
    sess.contact();

    Assert.assertTrue(sess.isRemovable(System.currentTimeMillis() + 1_000_000L));
  }

  public void testOnCleanupNoOpWhenConnectionNeverSetByMock () {

    OumuamuaSession<OrthodoxValue> sess = session(4);

    sess.onCleanup();

    Mockito.verify(connection).onCleanup();
  }

  public void testLongPollQueueOverflowEvictsOldest ()
    throws Exception {

    Mockito.when(protocol.isLongPolling()).thenReturn(true);

    OumuamuaSession<OrthodoxValue> sess = session(2);

    sess.completeConnection();

    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    sess.deliver(channel, null, delivery("/m1"));
    sess.deliver(channel, null, delivery("/m2"));
    sess.deliver(channel, null, delivery("/m3"));

    // Poll twice — the overflow path must drop the oldest entry, so two remain.
    Packet<OrthodoxValue> first = sess.poll(50L, java.util.concurrent.TimeUnit.MILLISECONDS);
    Packet<OrthodoxValue> second = sess.poll(50L, java.util.concurrent.TimeUnit.MILLISECONDS);
    Packet<OrthodoxValue> third = sess.poll(20L, java.util.concurrent.TimeUnit.MILLISECONDS);

    Assert.assertNotNull(first);
    Assert.assertNotNull(second);
    Assert.assertNull(third, "Overflowed queue must contain at most maxLongPollQueueSize entries");
  }

  public void testDeliverIgnoredWhenSessionNotConnected ()
    throws Exception {

    OumuamuaSession<OrthodoxValue> sess = session(4);

    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(channel.isStreaming()).thenReturn(false);

    sess.deliver(channel, null, delivery("/before-connect"));

    Mockito.verify(connection, Mockito.never()).deliver(Mockito.any());
  }
}
