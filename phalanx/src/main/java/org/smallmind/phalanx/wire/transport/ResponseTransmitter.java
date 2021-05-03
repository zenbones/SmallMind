package org.smallmind.phalanx.wire.transport;

public interface ResponseTransmitter {

  void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable;
}
