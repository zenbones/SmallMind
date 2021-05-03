package org.smallmind.phalanx.wire.transport.rest;

import java.util.Map;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.transport.AbstractRequestTransport;

public class RestRequestTransport extends AbstractRequestTransport {

  public RestRequestTransport (int defaultTimeoutSeconds) {

    super(defaultTimeoutSeconds);
  }

  @Override
  public String getCallerId () {

    return null;
  }

  @Override
  public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    return null;
  }

  @Override
  public void close () throws Exception {

  }
}
