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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

@Test(groups = "unit")
public class ChannelTreeTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;
  private ChannelRoot<OrthodoxValue> root;
  private ChannelTree<OrthodoxValue> tree;
  private List<Channel<OrthodoxValue>> created;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);
    Mockito.when(server.getCodec()).thenReturn(codec);

    root = new ChannelRoot<>(server);
    tree = new ChannelTree<>(root);
    created = new ArrayList<>();
  }

  private Channel<OrthodoxValue> create (String path)
    throws Exception {

    return tree.createIfAbsent(60_000L, 0, new DefaultRoute(path), created::add, (c, s) -> {
    }, (c, s) -> {
    }, null);
  }

  @SuppressWarnings("unchecked")
  private Session<OrthodoxValue> mockSession (String id) {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(session.getId()).thenReturn(id);

    return session;
  }

  public void testCreateIfAbsentInvokesCallbackOnce ()
    throws Exception {

    Channel<OrthodoxValue> first = create("/foo/bar");
    Channel<OrthodoxValue> second = create("/foo/bar");

    Assert.assertSame(second, first);
    Assert.assertEquals(created.size(), 1);
    Assert.assertSame(created.get(0), first);
  }

  public void testCreateIfAbsentAppliesInitializers ()
    throws Exception {

    AtomicInteger callCount = new AtomicInteger();
    java.util.Queue<org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer<OrthodoxValue>> queue = new java.util.LinkedList<>();

    queue.add(c -> {
      callCount.incrementAndGet();
      c.setPersistent(true);
    });

    Channel<OrthodoxValue> channel = tree.createIfAbsent(60_000L, 0, new DefaultRoute("/foo"), created::add, (c, s) -> {
    }, (c, s) -> {
    }, queue);

    Assert.assertEquals(callCount.get(), 1);
    Assert.assertTrue(channel.isPersistent());
    Channel<OrthodoxValue> again = tree.createIfAbsent(60_000L, 0, new DefaultRoute("/foo"), created::add, (c, s) -> {
    }, (c, s) -> {
    }, queue);

    Assert.assertSame(again, channel);
    Assert.assertEquals(callCount.get(), 1);
  }

  public void testFindReturnsCreatedChannel ()
    throws Exception {

    Channel<OrthodoxValue> created = create("/foo/bar/baz");

    Assert.assertSame(tree.find(0, new DefaultRoute("/foo/bar/baz")), created);
  }

  public void testFindUnknownReturnsNull ()
    throws Exception {

    create("/foo/bar");

    Assert.assertNull(tree.find(0, new DefaultRoute("/foo/qux")));
    Assert.assertNull(tree.find(0, new DefaultRoute("/missing")));
  }

  public void testCleanRemovesEmptyBranches ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo/bar/baz");

    tree.removeChannelIfPresent(0, new DefaultRoute("/foo/bar/baz"), removed -> {
    });
    tree.clean();

    Assert.assertNull(tree.find(0, new DefaultRoute("/foo/bar/baz")));

    Channel<OrthodoxValue> recreated = create("/foo/bar/baz");

    Assert.assertNotSame(recreated, channel);
  }

  public void testDeliverReachesLiteralSubscriber ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo/bar");
    Session<OrthodoxValue> session = mockSession("alice");

    channel.subscribe(session);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, new DefaultRoute("/foo/bar"), message);

    tree.deliver(null, 0, packet, new HashSet<>());

    Mockito.verify(session).deliver(Mockito.eq(channel), Mockito.isNull(), Mockito.any());
  }

  public void testDeliverReachesSingleWildcardSubscriber ()
    throws Exception {

    // Wildcard subscribers are only reached when the literal target branch exists in the
    // tree; routing navigates through literal segments and consults parent.childMap for '*'
    // only once index == route.size(). Production code path always creates the literal
    // channel before publishing, so we mirror that here.
    create("/foo/bar");

    Channel<OrthodoxValue> wildChannel = create("/foo/*");
    Session<OrthodoxValue> session = mockSession("bob");

    wildChannel.subscribe(session);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, new DefaultRoute("/foo/bar"), message);

    tree.deliver(null, 0, packet, new HashSet<>());

    Mockito.verify(session).deliver(Mockito.eq(wildChannel), Mockito.isNull(), Mockito.any());
  }

  public void testDeliverReachesDeepWildcardSubscriber ()
    throws Exception {

    Channel<OrthodoxValue> deepChannel = create("/foo/**");
    Session<OrthodoxValue> session = mockSession("carol");

    deepChannel.subscribe(session);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, new DefaultRoute("/foo/bar/baz"), message);

    tree.deliver(null, 0, packet, new HashSet<>());

    Mockito.verify(session).deliver(Mockito.eq(deepChannel), Mockito.isNull(), Mockito.any());
  }

  public void testDeliverFansOutToAllMatchingChannels ()
    throws Exception {

    Channel<OrthodoxValue> literal = create("/foo/bar");
    Channel<OrthodoxValue> wild = create("/foo/*");
    Channel<OrthodoxValue> deep = create("/foo/**");

    Session<OrthodoxValue> sA = mockSession("a");
    Session<OrthodoxValue> sB = mockSession("b");
    Session<OrthodoxValue> sC = mockSession("c");

    literal.subscribe(sA);
    wild.subscribe(sB);
    deep.subscribe(sC);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, new DefaultRoute("/foo/bar"), message);

    tree.deliver(null, 0, packet, new HashSet<>());

    Mockito.verify(sA).deliver(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(sB).deliver(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(sC).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testDeliverDedupesSubscriberAcrossWildcards ()
    throws Exception {

    Channel<OrthodoxValue> literal = create("/foo/bar");
    Channel<OrthodoxValue> wild = create("/foo/*");

    Session<OrthodoxValue> session = mockSession("dup");

    literal.subscribe(session);
    wild.subscribe(session);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, new DefaultRoute("/foo/bar"), message);

    tree.deliver(null, 0, packet, new HashSet<>());

    Mockito.verify(session, Mockito.times(1)).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }

  public void testDeliverIgnoresNonMatchingWildcardLevels ()
    throws Exception {

    Channel<OrthodoxValue> wild = create("/foo/*");
    Session<OrthodoxValue> session = mockSession("none");

    wild.subscribe(session);

    Message<OrthodoxValue> message = codec.create();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, new DefaultRoute("/foo/bar/baz"), message);

    tree.deliver(null, 0, packet, new HashSet<>());

    Mockito.verify(session, Mockito.never()).deliver(Mockito.any(), Mockito.any(), Mockito.any());
  }
}
