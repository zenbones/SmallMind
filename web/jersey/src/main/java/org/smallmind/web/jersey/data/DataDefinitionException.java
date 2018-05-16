package org.smallmind.web.jersey.data;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class DataDefinitionException extends FormattedException {

  public DataDefinitionException (String message, Object... args) {

    super(message, args);
  }
}
