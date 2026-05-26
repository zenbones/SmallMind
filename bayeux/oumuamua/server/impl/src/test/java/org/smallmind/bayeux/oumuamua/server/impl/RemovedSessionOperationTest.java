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

import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
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
public class RemovedSessionOperationTest {

  private ChannelTree<OrthodoxValue> tree;
  private Session<OrthodoxValue> session;

  @BeforeMethod
  public void beforeMethod () {

    Server<OrthodoxValue> server = Mockito.mock(Server.class);

    Mockito.when(server.getCodec()).thenReturn(new OrthodoxCodec(new JaxbDeserializer<>()));

    tree = new ChannelTree<>(new ChannelRoot<>(server));
    session = Mockito.mock(Session.class);
    Mockito.when(session.getId()).thenReturn("alice");
  }

  private Channel<OrthodoxValue> create (String path)
    throws Exception {

    return tree.createIfAbsent(60_000L, 0, new DefaultRoute(path), c -> {
    }, (c, s) -> {
    }, (c, s) -> {
    }, null);
  }

  public void testUnsubscribesCapturedSessionOnBranchWithChannel ()
    throws Exception {

    Channel<OrthodoxValue> channel = create("/foo");

    channel.subscribe(session);

    tree.walk(new RemovedSessionOperation<>(session));

    // Re-subscribing the same session reports a fresh subscription (returns true), confirming the previous subscription was removed.
    Assert.assertTrue(channel.subscribe(session));
  }

  public void testWalkAcrossTreeUnsubscribesEveryReachableChannel ()
    throws Exception {

    Channel<OrthodoxValue> foo = create("/foo");
    Channel<OrthodoxValue> deep = create("/foo/bar/baz");

    foo.subscribe(session);
    deep.subscribe(session);

    tree.walk(new RemovedSessionOperation<>(session));

    Assert.assertTrue(foo.subscribe(session));
    Assert.assertTrue(deep.subscribe(session));
  }

  public void testBranchWithoutChannelIsNoOp () {

    ChannelBranch<OrthodoxValue> empty = new ChannelBranch<>(null);

    new RemovedSessionOperation<>(session).operate(empty);
  }

  public void testOperateInvokesUnsubscribeWithCapturedSession () {

    ChannelBranch<OrthodoxValue> branch = Mockito.mock(ChannelBranch.class);
    Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);

    Mockito.when(branch.getChannel()).thenReturn(channel);

    new RemovedSessionOperation<>(session).operate(branch);

    Mockito.verify(channel).unsubscribe(session);
  }
}
