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
 * Specialization of {@link Connection} that binds the generic session lifecycle hooks to the
 * concrete {@link OumuamuaServer} and {@link OumuamuaSession} types through default methods,
 * eliminating boilerplate in every transport-specific connection implementation.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public interface OumuamuaConnection<V extends Value<V>> extends Connection<V> {

  /**
   * Creates a new {@link OumuamuaSession} for this connection and registers it in the server's
   * session map.
   *
   * @param server the owning server used to instantiate and register the session
   * @return the newly created and registered session
   */
  @Override
  default Session<V> createSession (Server<V> server) {

    OumuamuaSession<V> session = ((OumuamuaServer<V>)server).createSession(this);

    ((OumuamuaServer<V>)server).addSession(session);

    return session;
  }

  /**
   * Replaces the connection reference inside an existing session so that subsequent deliveries
   * flow through this connection; used when a client reconnects and reuses its session id.
   *
   * @param session the existing session whose connection should be replaced by this one
   */
  @Override
  default void hijackSession (Session<V> session) {

    ((OumuamuaSession<V>)session).hijack(this);
  }

  /**
   * Confirms that the session was created by the same protocol and transport combination as this
   * connection, guarding against cross-transport session re-use.
   *
   * @param session the session to validate against this connection's transport
   * @return {@code true} if the session's protocol and transport names both match
   */
  @Override
  default boolean validateSession (Session<V> session) {

    return getTransport().getProtocol().getName().equals(((OumuamuaSession<V>)session).getTransport().getProtocol().getName())
             && getTransport().getName().equals(((OumuamuaSession<V>)session).getTransport().getName());
  }

  /**
   * Refreshes the session's last-contact timestamp to prevent idle expiry.
   *
   * @param session the session whose idle timer should be reset
   */
  @Override
  default void updateSession (Session<V> session) {

    ((OumuamuaSession<V>)session).contact();
  }

  /**
   * Removes the session from the server's registry and unsubscribes it from all channels when the
   * underlying transport disconnects.
   *
   * @param server  the owning server from which the session should be deregistered
   * @param session the session that has disconnected
   */
  @Override
  default void onDisconnect (Server<V> server, Session<V> session) {

    ((OumuamuaServer<V>)server).removeSession((OumuamuaSession<V>)session);
  }
}
