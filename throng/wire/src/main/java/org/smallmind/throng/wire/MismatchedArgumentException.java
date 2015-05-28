package org.smallmind.throng.wire;

public class MismatchedArgumentException extends TransportException {

  public MismatchedArgumentException (String message, Object... args) {

    super(message, args);
  }
}