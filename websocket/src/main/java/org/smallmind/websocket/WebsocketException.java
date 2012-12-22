package org.smallmind.websocket;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class WebsocketException extends FormattedException {

  public WebsocketException (String message, Object... args) {

    super(message, args);
  }
}
