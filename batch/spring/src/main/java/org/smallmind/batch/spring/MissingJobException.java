package org.smallmind.batch.spring;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class MissingJobException extends FormattedRuntimeException {

  public MissingJobException (String message, Object... args) {

    super(message, args);
  }
}
