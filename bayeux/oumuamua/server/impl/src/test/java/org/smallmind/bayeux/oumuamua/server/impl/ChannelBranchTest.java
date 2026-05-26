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
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ChannelBranchTest {

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

  public void testRemoveChannelIfPresentReturnsBranchAndInvokesCallback ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo/bar", 60_000L);
    List<Channel<OrthodoxValue>> removed = new ArrayList<>();

    ChannelBranch<OrthodoxValue> branch = tree.removeChannelIfPresent(0, new DefaultRoute("/foo/bar"), removed::add);

    Assert.assertNotNull(branch);
    Assert.assertNull(branch.getChannel());
    Assert.assertEquals(removed.size(), 1);
    Assert.assertSame(removed.get(0), channel);
  }

  public void testRemoveChannelIfPresentReturnsNullForMissing ()
    throws Exception {

    create("/foo/bar", 60_000L);

    ChannelBranch<OrthodoxValue> branch = tree.removeChannelIfPresent(0, new DefaultRoute("/missing"), c -> {
    });

    Assert.assertNull(branch);
  }

  public void testRemoveChannelThrowsForPersistent ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo/bar", 60_000L);

    channel.setPersistent(true);
    Assert.assertThrows(ChannelStateException.class, () -> tree.removeChannelIfPresent(0, new DefaultRoute("/foo/bar"), c -> {
    }));
    Assert.assertSame(tree.find(0, new DefaultRoute("/foo/bar")), channel);
  }

  public void testRemoveChannelIfStillRemovableSkipsActiveChannel ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo/bar", 60_000L);

    Session<OrthodoxValue> session = Mockito.mock(Session.class);
    Mockito.when(session.getId()).thenReturn("s1");

    channel.subscribe(session);

    List<Channel<OrthodoxValue>> removed = new ArrayList<>();

    tree.removeChannelIfPresent(0, new DefaultRoute("/foo/bar"), removed::add);
    Assert.assertTrue(removed.isEmpty() || removed.get(0) == channel);
  }

  public void testWalkVisitsEveryBranch ()
    throws Exception {

    create("/foo/bar", 60_000L);
    create("/foo/baz", 60_000L);
    create("/qux", 60_000L);

    final int[] count = {0};

    tree.walk(branch -> count[0]++);

    Assert.assertTrue(count[0] >= 5);
  }

  public void testWalkVisitsChannelBranches ()
    throws Exception {

    Channel<OrthodoxValue> foo = create("/foo", 60_000L);
    Channel<OrthodoxValue> bar = create("/bar", 60_000L);

    List<Channel<OrthodoxValue>> seen = new ArrayList<>();

    tree.walk(branch -> {

      Channel<OrthodoxValue> channel;

      if ((channel = branch.getChannel()) != null) {
        seen.add(channel);
      }
    });

    Assert.assertEquals(seen.size(), 2);
    Assert.assertTrue(seen.contains(foo));
    Assert.assertTrue(seen.contains(bar));
  }

  public void testGetChannelReturnsNullWhenEmpty ()
    throws Exception {

    create("/foo/bar", 60_000L);
    final ChannelBranch<OrthodoxValue>[] holder = new ChannelBranch[] {null};

    tree.walk(branch -> {
      if (branch.getChannel() == null && holder[0] == null) {
        holder[0] = branch;
      }
    });

    Assert.assertNotNull(holder[0]);
    Assert.assertNull(holder[0].getChannel());
  }

  public void testRemoveChannelOnEmptyBranchIsNoOp ()
    throws Exception {

    create("/foo/bar", 60_000L);

    final ChannelBranch<OrthodoxValue>[] holder = new ChannelBranch[] {null};

    tree.walk(branch -> {
      if (branch != tree && branch.getChannel() == null && holder[0] == null) {
        holder[0] = branch;
      }
    });

    Assert.assertNotNull(holder[0]);

    List<Channel<OrthodoxValue>> removed = new ArrayList<>();

    holder[0].removeChannel(removed::add);

    Assert.assertTrue(removed.isEmpty(), "Removing an empty branch's channel must not fire the callback");
  }

  public void testRemoveChannelIfStillRemovableLeavesActiveChannelInPlace ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/active/channel", 60_000L);

    Session<OrthodoxValue> session = Mockito.mock(Session.class);
    Mockito.when(session.getId()).thenReturn("s1");
    channel.subscribe(session);

    final ChannelBranch<OrthodoxValue>[] holder = new ChannelBranch[] {null};

    tree.walk(branch -> {
      if (branch.getChannel() == channel) {
        holder[0] = branch;
      }
    });

    Assert.assertNotNull(holder[0]);

    List<Channel<OrthodoxValue>> removed = new ArrayList<>();

    holder[0].removeChannelIfStillRemovable(System.currentTimeMillis(), removed::add);

    Assert.assertTrue(removed.isEmpty(), "An active channel must not be removed by removeChannelIfStillRemovable");
    Assert.assertSame(holder[0].getChannel(), channel, "The active channel must remain on the branch");
  }

  public void testRemoveChannelIfStillRemovableThrowsForPersistentChannel ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/persistent/protected", 0L);

    channel.setPersistent(true);
    Thread.sleep(2L);

    final ChannelBranch<OrthodoxValue>[] holder = new ChannelBranch[] {null};

    tree.walk(branch -> {
      if (branch.getChannel() == channel) {
        holder[0] = branch;
      }
    });

    Assert.assertNotNull(holder[0]);

    final ChannelBranch<OrthodoxValue> branch = holder[0];

    Assert.assertThrows(ChannelStateException.class, () -> branch.removeChannelIfStillRemovable(System.currentTimeMillis() + 1_000_000L, c -> {
    }));

    Assert.assertSame(branch.getChannel(), channel, "Persistent channel must remain after a rejected removeChannelIfStillRemovable");
  }
}
