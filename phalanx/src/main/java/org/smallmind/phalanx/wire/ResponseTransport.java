package org.smallmind.phalanx.wire;

public interface ResponseTransport {

  public abstract String getInstanceId ();

  public abstract String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception;

  public abstract void transmit (String transportId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable;

  public abstract void close ()
    throws Exception;
}
