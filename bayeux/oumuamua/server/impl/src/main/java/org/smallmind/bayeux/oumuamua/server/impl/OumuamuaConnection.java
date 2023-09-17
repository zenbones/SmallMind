package org.smallmind.bayeux.oumuamua.server.impl;

import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;

public interface OumuamuaConnection<V extends Value<V>> extends Connection<V> {

  @Override
  default Session<V> createSession (Server<V> server) {

    OumuamuaSession<V> session = ((OumuamuaServer<V>)server).createSession(this);

    ((OumuamuaServer<V>)server).addSession(session);

    return session;
  }

  @Override
  default boolean validateSession (Session<V> session) {

    return getTransport().getProtocol().getName().equals(((OumuamuaSession<V>)session).getTransport().getProtocol().getName())
             && getTransport().getName().equals(((OumuamuaSession<V>)session).getTransport().getName());
  }

  @Override
  default void updateSession (Session<V> session) {

    ((OumuamuaSession<V>)session).contact();
  }

  @Override
  default void onDisconnect (Server<V> server, Session<V> session) {

    ((OumuamuaServer<V>)server).removeSession((OumuamuaSession<V>)session);
  }
}
