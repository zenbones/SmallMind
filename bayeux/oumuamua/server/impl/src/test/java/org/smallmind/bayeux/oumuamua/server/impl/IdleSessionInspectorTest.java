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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class IdleSessionInspectorTest {

  public void testStopAllowsRunToExitPromptly ()
    throws InterruptedException {

    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);
    IdleSessionInspector<OrthodoxValue> inspector = new IdleSessionInspector<>(server, 60L, Level.DEBUG);

    Thread runner = new Thread(inspector);

    runner.start();
    inspector.stop();
    runner.join(2_000L);
    Assert.assertFalse(runner.isAlive(), "run() did not exit within 2 seconds of stop()");
  }

  public void testIdleSessionIsRemovedAndDeparted ()
    throws InterruptedException {

    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);
    OumuamuaSession<OrthodoxValue> session = Mockito.mock(OumuamuaSession.class);
    List<OumuamuaSession<OrthodoxValue>> sessions = new ArrayList<>();
    CountDownLatch cleanupLatch = new CountDownLatch(1);

    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.checkAndDisconnect(Mockito.anyLong())).thenReturn(true);
    Mockito.doAnswer(invocation -> {
      cleanupLatch.countDown();

      return null;
    }).when(session).onCleanup();

    sessions.add(session);
    Mockito.when(server.iterateSessions()).thenAnswer(invocation -> {

      Iterator<OumuamuaSession<OrthodoxValue>> iterator = sessions.iterator();

      return iterator;
    });

    IdleSessionInspector<OrthodoxValue> inspector = new IdleSessionInspector<>(server, 0L, Level.DEBUG);
    Thread runner = new Thread(inspector);

    runner.start();
    Assert.assertTrue(cleanupLatch.await(2L, TimeUnit.SECONDS), "session cleanup did not run within 2 seconds");
    inspector.stop();
    runner.join(2_000L);

    Mockito.verify(server, Mockito.atLeastOnce()).departChannels(session);
    Mockito.verify(session, Mockito.atLeastOnce()).onCleanup();
    Assert.assertTrue(sessions.isEmpty(), "iterator.remove() should have cleared the session list");
  }

  public void testNonIdleSessionIsLeftAlone ()
    throws InterruptedException {

    OumuamuaServer<OrthodoxValue> server = Mockito.mock(OumuamuaServer.class);
    OumuamuaSession<OrthodoxValue> session = Mockito.mock(OumuamuaSession.class);
    List<OumuamuaSession<OrthodoxValue>> sessions = new ArrayList<>();
    CountDownLatch sweepLatch = new CountDownLatch(1);

    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.checkAndDisconnect(Mockito.anyLong())).thenAnswer(invocation -> {
      sweepLatch.countDown();

      return false;
    });

    sessions.add(session);
    Mockito.when(server.iterateSessions()).thenAnswer(invocation -> sessions.iterator());

    IdleSessionInspector<OrthodoxValue> inspector = new IdleSessionInspector<>(server, 0L, Level.DEBUG);
    Thread runner = new Thread(inspector);

    runner.start();
    Assert.assertTrue(sweepLatch.await(2L, TimeUnit.SECONDS), "checkAndDisconnect was not invoked within 2 seconds");
    inspector.stop();
    runner.join(2_000L);

    Mockito.verify(server, Mockito.never()).departChannels(session);
    Mockito.verify(session, Mockito.never()).onCleanup();
    Assert.assertEquals(sessions.size(), 1, "non-idle session should remain in the list");
  }
}
