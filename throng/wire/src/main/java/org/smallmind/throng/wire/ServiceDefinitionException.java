package org.smallmind.throng.wire;

public class ServiceDefinitionException extends TransportException {

  public ServiceDefinitionException (String message, Object... args) {

    super(message, args);
  }
}
