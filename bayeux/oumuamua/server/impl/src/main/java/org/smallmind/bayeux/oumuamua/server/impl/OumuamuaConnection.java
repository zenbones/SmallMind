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

import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;

/**
 * Connection contract specialized for the Oumuamua server, providing default session lifecycle hooks.
 *
 * @param <V> value representation
 */
public interface OumuamuaConnection<V extends Value<V>> extends Connection<V> {

  /**
   * Creates and registers a new session with the server for this connection.
   *
   * @param server server creating the session
   * @return newly created session
   */
  @Override
  default Session<V> createSession (Server<V> server) {

    OumuamuaSession<V> session = ((OumuamuaServer<V>)server).createSession(this);

    ((OumuamuaServer<V>)server).addSession(session);

    return session;
  }

  /**
   * Rebinds an existing session to this connection.
   *
   * @param session session being hijacked
   */
  @Override
  default void hijackSession (Session<V> session) {

    ((OumuamuaSession<V>)session).hijack(this);
  }

  /**
   * Validates that the supplied session belongs to the same protocol and transport.
   *
   * @param session session to validate
   * @return {@code true} if compatible
   */
  @Override
  default boolean validateSession (Session<V> session) {

    return getTransport().getProtocol().getName().equals(((OumuamuaSession<V>)session).getTransport().getProtocol().getName())
             && getTransport().getName().equals(((OumuamuaSession<V>)session).getTransport().getName());
  }

  /**
   * Updates the session's last-contact time.
   *
   * @param session session to refresh
   */
  @Override
  default void updateSession (Session<V> session) {

    ((OumuamuaSession<V>)session).contact();
  }

  /**
   * Removes the session from the server on disconnect.
   *
   * @param server  owning server
   * @param session session that disconnected
   */
  @Override
  default void onDisconnect (Server<V> server, Session<V> session) {

    ((OumuamuaServer<V>)server).removeSession((OumuamuaSession<V>)session);
  }
}
