package org.smallmind.phalanx.worker;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class WorkManagerException extends FormattedRuntimeException {

  public WorkManagerException (String message, Object... args) {

    super(message, args);
  }
}
