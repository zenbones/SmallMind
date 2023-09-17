package org.smallmind.bayeux.oumuamua.server.impl;

import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;

public abstract class OumuamuaConnection<V extends Value<V>> implements Connection<V> {

  private final Transport<V> transport;

  public OumuamuaConnection (Transport<V> transport) {

    this.transport = transport;
  }

  @Override
  public Transport<V> getTransport () {

    return transport;
  }

  @Override
  public Session<V> createSession (Server<V> server) {

    OumuamuaSession<V> session = ((OumuamuaServer<V>)server).createSession(this);

    ((OumuamuaServer<V>)server).addSession(session);

    return session;
  }

  @Override
  public boolean validateSession (Session<V> session) {

    return getTransport().getProtocol().getName().equals(((OumuamuaSession<V>)session).getTransport().getProtocol().getName())
             && getTransport().getName().equals(((OumuamuaSession<V>)session).getTransport().getName());
  }

  @Override
  public void updateSession (Session<V> session) {

    ((OumuamuaSession<V>)session).contact();
  }

  @Override
  public void onDisconnect (Server<V> server, Session<V> session) {

    ((OumuamuaServer<V>)server).removeSession((OumuamuaSession<V>)session);
  }
}
