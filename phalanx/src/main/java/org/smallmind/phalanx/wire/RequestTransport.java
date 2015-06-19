package org.smallmind.phalanx.wire;

import java.util.Map;

public interface RequestTransport {

  public abstract String getTransportId ();

  public abstract void transmitInOnly (Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable;

  public abstract Object transmitInOut (Address address, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable;

  public abstract void close ()
    throws Exception;
}
