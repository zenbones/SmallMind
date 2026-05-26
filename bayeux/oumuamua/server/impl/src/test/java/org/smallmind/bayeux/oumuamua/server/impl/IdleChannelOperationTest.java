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
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class IdleChannelOperationTest {

  private Server<OrthodoxValue> server;
  private ChannelTree<OrthodoxValue> tree;

  @BeforeMethod
  public void beforeMethod () {

    server = Mockito.mock(Server.class);
    Mockito.when(server.getCodec()).thenReturn(new OrthodoxCodec(new JaxbDeserializer<>()));

    tree = new ChannelTree<>(new ChannelRoot<>(server));
  }

  private Channel<OrthodoxValue> create (String path, long ttl)
    throws Exception {

    return tree.createIfAbsent(ttl, 0, new DefaultRoute(path), c -> {
    }, (c, s) -> {
    }, (c, s) -> {
    }, null);
  }

  public void testRemovesIdleChannel ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo", 0L);

    List<Channel<OrthodoxValue>> removed = new ArrayList<>();

    Thread.sleep(2L);
    tree.walk(new IdleChannelOperation<>(System.currentTimeMillis(), Level.DEBUG, removed::add));

    Assert.assertEquals(removed.size(), 1);
    Assert.assertSame(removed.get(0), channel);
    Assert.assertNull(tree.find(0, new DefaultRoute("/foo")));
  }

  public void testSkipsPersistentChannel ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo", 0L);

    channel.setPersistent(true);

    List<Channel<OrthodoxValue>> removed = new ArrayList<>();

    tree.walk(new IdleChannelOperation<>(System.currentTimeMillis() + 60_000L, Level.DEBUG, removed::add));

    Assert.assertTrue(removed.isEmpty());
    Assert.assertSame(tree.find(0, new DefaultRoute("/foo")), channel);
  }

  public void testSkipsChannelWithinTtl ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo", 60_000L);

    List<Channel<OrthodoxValue>> removed = new ArrayList<>();

    tree.walk(new IdleChannelOperation<>(System.currentTimeMillis(), Level.DEBUG, removed::add));

    Assert.assertTrue(removed.isEmpty());
    Assert.assertSame(tree.find(0, new DefaultRoute("/foo")), channel);
  }
}
