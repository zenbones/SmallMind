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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class IdleChannelSifterTest {

  @SuppressWarnings("unchecked")
  public void testStopAllowsRunToExitPromptly ()
    throws InterruptedException {

    Server<OrthodoxValue> server = Mockito.mock(Server.class);
    Mockito.when(server.getCodec()).thenReturn(new OrthodoxCodec(new JaxbDeserializer<>()));

    ChannelTree<OrthodoxValue> tree = new ChannelTree<>(new ChannelRoot<>(server));
    IdleChannelSifter<OrthodoxValue> sifter = new IdleChannelSifter<>(60L, Level.DEBUG, tree, c -> {
    });

    Thread runner = new Thread(sifter);

    runner.start();
    sifter.stop();
    runner.join(2_000L);
    Assert.assertFalse(runner.isAlive(), "run() did not exit within 2 seconds of stop()");
  }

  @SuppressWarnings("unchecked")
  public void testCycleRemovesIdleChannel ()
    throws Exception {

    Server<OrthodoxValue> server = Mockito.mock(Server.class);

    Mockito.when(server.getCodec()).thenReturn(new OrthodoxCodec(new JaxbDeserializer<>()));

    ChannelTree<OrthodoxValue> tree = new ChannelTree<>(new ChannelRoot<>(server));
    Channel<OrthodoxValue> channel = tree.createIfAbsent(0L, 0, new DefaultRoute("/foo"), c -> {
    }, (c, s) -> {
    }, (c, s) -> {
    }, null);

    CountDownLatch removed = new CountDownLatch(1);
    AtomicInteger removalCount = new AtomicInteger();

    IdleChannelSifter<OrthodoxValue> sifter = new IdleChannelSifter<>(0L, Level.DEBUG, tree, c -> {
      removalCount.incrementAndGet();
      removed.countDown();
    });

    Thread.sleep(2L);

    Thread runner = new Thread(sifter);

    runner.start();
    Assert.assertTrue(removed.await(2L, TimeUnit.SECONDS), "channel removal callback did not fire within 2 seconds");
    sifter.stop();
    runner.join(2_000L);

    Assert.assertEquals(removalCount.get(), 1);
    Assert.assertNull(tree.find(0, new DefaultRoute("/foo")));
    Assert.assertNotNull(channel);
  }

  @SuppressWarnings("unchecked")
  public void testCycleKeepsActiveChannel ()
    throws Exception {

    Server<OrthodoxValue> server = Mockito.mock(Server.class);

    Mockito.when(server.getCodec()).thenReturn(new OrthodoxCodec(new JaxbDeserializer<>()));

    ChannelTree<OrthodoxValue> tree = new ChannelTree<>(new ChannelRoot<>(server));
    Channel<OrthodoxValue> channel = tree.createIfAbsent(60_000L, 0, new DefaultRoute("/foo"), c -> {
    }, (c, s) -> {
    }, (c, s) -> {
    }, null);

    AtomicInteger removalCount = new AtomicInteger();
    IdleChannelSifter<OrthodoxValue> sifter = new IdleChannelSifter<>(0L, Level.DEBUG, tree, c -> removalCount.incrementAndGet());
    Thread runner = new Thread(sifter);

    runner.start();
    Thread.sleep(50L);
    sifter.stop();
    runner.join(2_000L);

    Assert.assertEquals(removalCount.get(), 0);
    Assert.assertSame(tree.find(0, new DefaultRoute("/foo")), channel);
  }
}
