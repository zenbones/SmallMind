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

import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.spi.meta.Meta;

public interface Connection<V extends Value<V>> {

  default Packet<V> respond (Protocol<V> protocol, Server<V> server, Session<V> session, Message<V> request) {

    try {

      String path= request.getChannel();
      Meta meta = Meta.from(path);
      Route route = Meta.PUBLISH.equals(meta) ? new DefaultRoute(path) : meta.getRoute();
      Packet<V> response;

      server.onRequest(session, new Packet<>(PacketType.REQUEST, session.getId(), route, request));

      response = meta.process(protocol, route, server, session, request);

      server.onResponse(session, response);
      session.onResponse(session, response);

      return response;
    } catch (InterruptedException | InvalidPathException | MetaProcessingException exception) {
      return new Packet<>(PacketType.RESPONSE, request.getSessionId(), null, Meta.constructErrorResponse(server, request.getChannel(), request.getId(), request.getSessionId(), exception.getMessage(), null));
    }
  }

  Transport<V> getTransport ();

  void maintenance ();

  void deliver (Packet<V> packet);
}
