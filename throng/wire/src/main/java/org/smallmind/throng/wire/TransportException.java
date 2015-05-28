package org.smallmind.throng.wire;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class TransportException extends FormattedException {

  public TransportException () {

    super();
  }

  public TransportException (String message, Object... args) {

    super(message, args);
  }

  public TransportException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  public TransportException (Throwable throwable) {

    super(throwable);
  }
}