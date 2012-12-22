package org.smallmind.websocket;

public class SyntaxException extends WebsocketException {

  public SyntaxException (String message, Object... args) {

    super(message, args);
  }
}
