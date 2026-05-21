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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies the {@link ChannelInitializer} contract that {@link OumuamuaServer#requireChannel}
 * ultimately enforces through the channel tree: registered initializers run exactly once,
 * at the moment a channel is created, in registration order, with the newly created
 * {@link Channel} as the only argument. Looking up an already-existing channel must not
 * re-trigger the chain.
 */
@Test(groups = "unit")
public class ChannelInitializerTest {

  private Server<OrthodoxValue> server;
  private ChannelTree<OrthodoxValue> tree;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    server = Mockito.mock(Server.class);
    Mockito.when(server.getCodec()).thenReturn(new OrthodoxCodec(new JaxbDeserializer<>()));

    tree = new ChannelTree<>(new ChannelRoot<>(server));
  }

  private Channel<OrthodoxValue> create (String path, Queue<ChannelInitializer<OrthodoxValue>> initializers)
    throws Exception {

    return tree.createIfAbsent(60_000L, 0, new DefaultRoute(path), c -> {
    }, (c, s) -> {
    }, (c, s) -> {
    }, initializers);
  }

  public void testInitializerRunsOnceOnCreationWithTheNewChannel ()
    throws Exception {

    AtomicInteger invocationCount = new AtomicInteger(0);
    List<Channel<OrthodoxValue>> seen = new ArrayList<>();
    Queue<ChannelInitializer<OrthodoxValue>> initializers = new LinkedBlockingDeque<>();

    initializers.add(channel -> {
      invocationCount.incrementAndGet();
      seen.add(channel);
    });

    Channel<OrthodoxValue> channel = create("/init/one", initializers);

    Assert.assertNotNull(channel);
    Assert.assertEquals(invocationCount.get(), 1, "Initializer should run exactly once on creation");
    Assert.assertSame(seen.get(0), channel, "Initializer should be invoked with the newly created channel");
  }

  public void testMultipleInitializersRunInRegistrationOrder ()
    throws Exception {

    List<String> order = new ArrayList<>();
    Queue<ChannelInitializer<OrthodoxValue>> initializers = new LinkedBlockingDeque<>();

    initializers.add(channel -> order.add("first"));
    initializers.add(channel -> order.add("second"));
    initializers.add(channel -> order.add("third"));

    create("/init/order", initializers);

    Assert.assertEquals(order, List.of("first", "second", "third"), "Initializers must run in registration order");
  }

  public void testInitializerDoesNotRunAgainWhenChannelAlreadyExists ()
    throws Exception {

    AtomicInteger invocationCount = new AtomicInteger(0);
    Queue<ChannelInitializer<OrthodoxValue>> initializers = new LinkedBlockingDeque<>();

    initializers.add(channel -> invocationCount.incrementAndGet());

    Channel<OrthodoxValue> first = create("/init/idempotent", initializers);
    Channel<OrthodoxValue> second = create("/init/idempotent", initializers);

    Assert.assertSame(second, first, "Second call must return the existing channel");
    Assert.assertEquals(invocationCount.get(), 1, "Initializer must not run again for an existing channel");
  }

  public void testInitializerCanConfigureChannelBeforeReturn ()
    throws Exception {

    Queue<ChannelInitializer<OrthodoxValue>> initializers = new LinkedBlockingDeque<>();

    initializers.add(channel -> channel.setPersistent(true));

    Channel<OrthodoxValue> channel = create("/init/persistent", initializers);

    Assert.assertTrue(channel.isPersistent(), "Initializer should have flipped the persistent flag before the channel was handed back");
  }

  public void testNullInitializerQueueIsAccepted ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/init/none", null);

    Assert.assertNotNull(channel, "Channel creation must work without any initializers");
  }
}
