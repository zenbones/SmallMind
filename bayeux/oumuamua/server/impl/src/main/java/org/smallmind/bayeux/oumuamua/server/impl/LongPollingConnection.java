package org.smallmind.bayeux.oumuamua.server.impl;

import javax.servlet.AsyncContext;
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;

public class LongPollingConnection<V extends Value<V>> implements Connection<V> {

  private final LongPollingTransport<V> longPollingTransport;

  public LongPollingConnection (LongPollingTransport<V> longPollingTransport) {

    this.longPollingTransport = longPollingTransport;
  }

  @Override
  public Transport getTransport () {

    return longPollingTransport;
  }

  @Override
  public void deliver (Packet<V> packet) {

    throw new UnsupportedOperationException();
  }

  public void onMessages (AsyncContext asyncContext, Message<V>[] messages) {

  }
}
