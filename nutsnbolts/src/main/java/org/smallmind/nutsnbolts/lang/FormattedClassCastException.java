package org.smallmind.nutsnbolts.lang;

public class FormattedClassCastException extends ClassCastException {

  public FormattedClassCastException () {

    super();
  }

  public FormattedClassCastException (String message, Object... args) {

    super(String.format(message, args));
  }
}
