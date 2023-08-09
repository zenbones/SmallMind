package org.smallmind.cometd.oumuamua.v1.json.jackson;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class InvalidJsonNodeType extends FormattedRuntimeException {

  public InvalidJsonNodeType (String message, Object... args) {

    super(message, args);
  }
}
