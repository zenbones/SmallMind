package org.smallmind.web.websocket.spi;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class SessionRuntimeException extends FormattedRuntimeException {

  public SessionRuntimeException (Throwable throwable) {

    super(throwable);
  }
}
