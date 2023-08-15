package org.smallmind.bayeux.oumuamua.server.api;

public class IllegalChannelStateException extends OumuamuaException {

  public IllegalChannelStateException (String message, Object... args) {

    super(message, args);
  }
}
