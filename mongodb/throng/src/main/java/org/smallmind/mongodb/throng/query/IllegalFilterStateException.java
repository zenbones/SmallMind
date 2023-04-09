package org.smallmind.mongodb.throng.query;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class IllegalFilterStateException extends FormattedRuntimeException {

  public IllegalFilterStateException (String message, Object... args) {

    super(message, args);
  }
}
