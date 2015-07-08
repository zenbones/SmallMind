package org.smallmind.phalanx.wire;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class MissingInstanceIdException extends FormattedException {

  public MissingInstanceIdException (String message, Object... args) {

    super(message, args);
  }

  public MissingInstanceIdException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  public MissingInstanceIdException (Throwable throwable) {

    super(throwable);
  }
}
