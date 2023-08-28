package org.smallmind.bayeux.oumuamua.server.spi.longpolling;

import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Transport;

public class ServletProtocol implements Protocol {

  @Override
  public String getName () {

    return null;
  }

  @Override
  public boolean isLongPolling () {

    return false;
  }

  @Override
  public long getLongPollIntervalMilliseconds () {

    return 0;
  }

  @Override
  public long getLongPollTimeoutMilliseconds () {

    return 0;
  }

  @Override
  public String[] getTransportNames () {

    return new String[0];
  }

  @Override
  public Transport getTransport (String name) {

    return null;
  }
}
