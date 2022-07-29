package org.smallmind.scribe.pen;

import org.smallmind.nutsnbolts.lang.FormattedIOException;

public class FluentBitConnectionException extends FormattedIOException {

  public FluentBitConnectionException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }
}
