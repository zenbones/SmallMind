package org.smallmind.sleuth;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class TestDefinitionException extends FormattedRuntimeException {

  public TestDefinitionException (String message, Object... args) {

    super(message, args);
  }
}
