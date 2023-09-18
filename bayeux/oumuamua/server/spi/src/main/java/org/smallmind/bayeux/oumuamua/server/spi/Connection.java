/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.bayeux.oumuamua.server.spi;

import java.io.IOException;
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;

public interface Connection<V extends Value<V>> {

  default void process (Server<V> server, ResponseConsumer<V> responseConsumer, Message<V>[] messages) {

    for (Message<V> message : messages) {

      Packet<V> packet;
      String path = message.getChannel();
      String sessionId = message.getSessionId();

      try {

        Meta meta = Meta.from(path);
        Route route = Meta.PUBLISH.equals(meta) ? new DefaultRoute(path) : meta.getRoute();

        if (sessionId == null) {
          if (Meta.HANDSHAKE.equals(meta)) {

            Session<V> session = createSession(server);

            if (SessionState.DISCONNECTED.equals(session.getState())) {
              throw new MetaProcessingException("Session has been disconnected");
            } else {
              packet = cycle(meta, route, server, session, message);
            }
          } else {
            throw new MetaProcessingException("Missing client id");
          }
        } else {

          Session<V> session;

          if ((session = server.getSession(sessionId)) == null) {
            throw new MetaProcessingException("Invalid client id");
          } else if (!validateSession(session)) {
            throw new MetaProcessingException("Invalid session type");
          } else if (SessionState.DISCONNECTED.equals(session.getState())) {
            throw new MetaProcessingException("Session has been disconnected");
          } else {
            packet = cycle(meta, route, server, session, message);
          }
        }
      } catch (IOException | InterruptedException | InvalidPathException | MetaProcessingException exception) {
        packet = new Packet<>(PacketType.RESPONSE, sessionId, null, Meta.constructErrorResponse(server, path, message.getId(), sessionId, exception.getMessage(), null));
      }

      responseConsumer.accept(packet);
    }
  }

  private Packet<V> cycle (Meta meta, Route route, Server<V> server, Session<V> session, Message<V> request)
    throws IOException, InterruptedException, InvalidPathException {

    Packet<V> packet;

    updateSession(session);

    packet = respond(meta, route, server, session, request);

    if (SessionState.DISCONNECTED.equals(session.getState())) {
      onDisconnect(server, session);
    }

    return packet;
  }

  private Packet<V> respond (Meta meta, Route route, Server<V> server, Session<V> session, Message<V> request)
    throws InterruptedException, InvalidPathException {

    Packet<V> response;

    server.onRequest(session, new Packet<>(PacketType.REQUEST, session.getId(), route, request));

    response = meta.process(getTransport().getProtocol(), route, server, session, request);

    server.onResponse(session, response);
    session.onResponse(session, response);

    return response;
  }

  Transport<V> getTransport ();

  Session<V> createSession (Server<V> server);

  boolean validateSession (Session<V> session);

  void updateSession (Session<V> session);

  void onDisconnect (Server<V> server, Session<V> session);

  void deliver (Packet<V> packet);
}
