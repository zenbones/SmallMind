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

import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Service contract for handling Bayeux messages routed to the server.
 *
 * @param <V> concrete {@link Value} implementation used to represent JSON payloads
 */
public interface BayeuxService<V extends Value<V>> {

  /**
   * Creates a basic response message that mirrors request metadata and binds to the route.
   *
   * @param route   the route being processed
   * @param server  the hosting server instance
   * @param session the client session issuing the request
   * @param request the incoming message that triggered the response
   * @return a new response message pre-populated with channel, id, and session identifiers
   */
  default Message<V> createResponse (Route route, Server<V> server, Session<V> session, Message<V> request) {

    Message<V> response = (Message<V>)server.getCodec().create().put(Message.CHANNEL, route.getPath()).put(Message.ID, request.getId()).put(Message.SESSION_ID, session.getId());

    return response;
  }

  /**
   * Provides the channel routes this service is bound to handle.
   *
   * @return an array of bound routes
   */
  Route[] getBoundRoutes ();

  /**
   * Processes an incoming message for the given route.
   *
   * @param protocol the transport protocol in use
   * @param route    the target route
   * @param server   the owning server
   * @param session  the current client session
   * @param request  the incoming message
   * @return a packet response to send back to the client
   */
  Packet<V> process (Protocol<V> protocol, Route route, Server<V> server, Session<V> session, Message<V> request);
}
