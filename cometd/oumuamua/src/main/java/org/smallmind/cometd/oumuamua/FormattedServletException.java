package org.smallmind.cometd.oumuamua;

import javax.servlet.ServletException;

public class FormattedServletException extends ServletException {

  public FormattedServletException () {

    super();
  }

  public FormattedServletException (String message, Object... args) {

    super(message == null ? null : String.format(message, args));
  }

  public FormattedServletException (Throwable throwable, String message, Object... args) {

    super(message == null ? null : String.format(message, args), throwable);
  }

  public FormattedServletException (Throwable throwable) {

    super(throwable);
  }
}
