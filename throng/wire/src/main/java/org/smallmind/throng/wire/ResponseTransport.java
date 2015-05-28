package org.smallmind.throng.wire;

public interface ResponseTransport {

  public abstract String getInstanceId ();

  public abstract void register (Class<?> serviceInterface, WiredService targetService)
    throws Exception;

  public abstract void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable;

  public abstract void close ()
    throws Exception;
}
