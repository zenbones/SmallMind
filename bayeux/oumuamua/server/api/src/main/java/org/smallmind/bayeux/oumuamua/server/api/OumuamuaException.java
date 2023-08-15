package org.smallmind.bayeux.oumuamua.server.api;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class OumuamuaException extends FormattedException {

  public OumuamuaException (String message, Object... args) {

    super(message, args);
  }

  public OumuamuaException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  public OumuamuaException (Throwable throwable) {

    super(throwable);
  }
}
