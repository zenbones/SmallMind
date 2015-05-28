package org.smallmind.throng.wire;

public class TransportTimeoutException extends TransportException {

  public TransportTimeoutException (String message, Object... args) {

    super(message, args);
  }
}