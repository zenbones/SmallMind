package org.smallmind.instrument.aop;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class MissingMetricException extends FormattedRuntimeException {

  public MissingMetricException (String message, Object... args) {

    super(message, args);
  }
}
