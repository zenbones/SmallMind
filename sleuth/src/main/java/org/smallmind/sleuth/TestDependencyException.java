package org.smallmind.sleuth;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class TestDependencyException extends FormattedRuntimeException {

  public TestDependencyException (String message, Object... args) {

    super(message, args);
  }
}
