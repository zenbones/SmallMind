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
package org.smallmind.bayeux.oumuamua.server.api;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Named physical connection mechanism (e.g., long-polling or WebSocket) owned by a {@link Protocol}
 * and responsible for the actual byte-level exchange with clients.
 *
 * @param <V> concrete {@link Value} implementation used for message payloads
 */
public interface Transport<V extends Value<V>> extends Attributed {

  /**
   * Returns the protocol that owns this transport.
   *
   * @return owning protocol
   */
  Protocol<V> getProtocol ();

  /**
   * Returns the name used during Bayeux connection-type negotiation.
   *
   * @return transport name string
   */
  String getName ();

  /**
   * Returns whether this transport is restricted to local (in-process) use and should not
   * be offered to remote clients.
   *
   * @return {@code true} if the transport is local-only
   */
  boolean isLocal ();

  /**
   * Initializes this transport using servlet and server context.
   *
   * @param server        hosting server
   * @param servletConfig servlet configuration
   * @throws ServletException if transport initialization fails
   */
  void init (Server<?> server, ServletConfig servletConfig)
    throws ServletException;
}
