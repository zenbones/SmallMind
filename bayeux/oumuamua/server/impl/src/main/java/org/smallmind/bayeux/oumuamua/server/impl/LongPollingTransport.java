package org.smallmind.bayeux.oumuamua.server.impl;

import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.Transports;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.longpolling.ServletProtocol;

public class LongPollingTransport<V extends Value<V>> extends AbstractAttributed implements Transport {

  private final ServletProtocol servletProtocol;

  public LongPollingTransport (ServletProtocol servletProtocol) {

    this.servletProtocol = servletProtocol;
  }

  @Override
  public Protocol getProtocol () {

    return servletProtocol;
  }

  @Override
  public String getName () {

    return Transports.LONG_POLLING.getName();
  }

  protected Connection<V> createConnections () {

    return new LongPollingConnection<>(this);
  }
}
