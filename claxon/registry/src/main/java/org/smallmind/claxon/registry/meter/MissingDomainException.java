package org.smallmind.claxon.registry.meter;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class MissingDomainException extends FormattedRuntimeException {

  public MissingDomainException (String message, Object... args) {

    super(message, args);
  }
}
