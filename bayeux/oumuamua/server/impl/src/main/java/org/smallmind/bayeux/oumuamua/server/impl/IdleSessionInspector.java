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

import java.util.Iterator;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Background {@link Runnable} that iterates the active session registry, disconnects any session
 * that has been idle beyond its configured timeout, removes it from the server, and departs it from
 * all channels. Scheduled on a fixed cadence by {@link OumuamuaServer}, which owns its lifecycle.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class IdleSessionInspector<V extends Value<V>> implements Runnable {

  private final OumuamuaServer<V> server;
  private final Level idleSessionLogLevel;

  /**
   * Creates an inspector bound to the given server.
   *
   * @param server              the server whose session map will be inspected
   * @param idleSessionLogLevel log level at which idle-session termination events are recorded
   */
  public IdleSessionInspector (OumuamuaServer<V> server, Level idleSessionLogLevel) {

    this.server = server;
    this.idleSessionLogLevel = idleSessionLogLevel;
  }

  /**
   * Performs a single maintenance pass: iterates all active sessions and for each one that has
   * exceeded its idle timeout atomically checks and transitions it to the disconnected state via
   * {@link OumuamuaSession#checkAndDisconnect}, removes it from the registry via the iterator,
   * departs it from all channels, and triggers connection cleanup. Any failure of the pass is logged
   * so that it cannot cancel future runs.
   */
  @Override
  public void run () {

    try {

      Iterator<OumuamuaSession<V>> sessionIterator = server.iterateSessions();
      long now = System.currentTimeMillis();

      while (sessionIterator.hasNext()) {

        OumuamuaSession<V> session = sessionIterator.next();

        if (session.checkAndDisconnect(now)) {
          LoggerManager.getLogger(IdleSessionInspector.class).log(idleSessionLogLevel, "Idle session termination(%s)", session.getId());

          sessionIterator.remove();
          server.departChannels(session);
          session.onCleanup();
        }
      }
    } catch (Exception exception) {
      LoggerManager.getLogger(IdleSessionInspector.class).error(exception);
    }
  }
}
