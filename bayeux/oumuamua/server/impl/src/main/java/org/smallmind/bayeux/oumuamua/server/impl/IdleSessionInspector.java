/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Background inspector that disconnects and cleans up idle sessions.
 *
 * @param <V> value representation
 */
public class IdleSessionInspector<V extends Value<V>> implements Runnable {

  private final CountDownLatch finishLatch = new CountDownLatch(1);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final OumuamuaServer<V> server;
  private final Level idleSessionLogLevel;
  private final long connectionMaintenanceCycleMinutes;

  /**
   * Constructs an inspector that periodically evaluates sessions for idleness.
   *
   * @param server owning server
   * @param connectionMaintenanceCycleMinutes cadence for maintenance checks
   * @param idleSessionLogLevel log level used when terminating idle sessions
   */
  public IdleSessionInspector (OumuamuaServer<V> server, long connectionMaintenanceCycleMinutes, Level idleSessionLogLevel) {

    this.server = server;
    this.connectionMaintenanceCycleMinutes = connectionMaintenanceCycleMinutes;
    this.idleSessionLogLevel = idleSessionLogLevel;
  }

  /**
   * Requests shutdown and waits for the worker to exit.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  public void stop ()
    throws InterruptedException {

    finishLatch.countDown();
    exitLatch.await();
  }

  /**
   * Loops until stopped, terminating idle sessions and cleaning up their channels.
   */
  @Override
  public void run () {

    try {
      while (!finishLatch.await(connectionMaintenanceCycleMinutes, TimeUnit.MINUTES)) {

        Iterator<OumuamuaSession<V>> sessionIterator = server.iterateSessions();
        long now = System.currentTimeMillis();

        while (sessionIterator.hasNext()) {

          OumuamuaSession<V> session = sessionIterator.next();

          if (session.isRemovable(now)) {
            LoggerManager.getLogger(IdleSessionInspector.class).log(idleSessionLogLevel, "Idle session termination(%s)", session.getId());

            session.completeDisconnect();
            sessionIterator.remove();
            server.departChannels(session);
            session.onCleanup();
          }
        }
      }
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(OumuamuaServer.class).error(interruptedException);
    } finally {
      exitLatch.countDown();
    }
  }
}
