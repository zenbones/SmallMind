package org.smallmind.throng.wire;

public class MissingInvocationException extends TransportException {

  public MissingInvocationException (String message, Object... args) {

    super(message, args);
  }
}