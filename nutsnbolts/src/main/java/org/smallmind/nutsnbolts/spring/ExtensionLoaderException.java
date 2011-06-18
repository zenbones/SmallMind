package org.smallmind.nutsnbolts.spring;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ExtensionLoaderException extends FormattedException {

  public ExtensionLoaderException () {

    super();
  }

  public ExtensionLoaderException (String message, Object... args) {

    super(message, args);
  }

  public ExtensionLoaderException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  public ExtensionLoaderException (Throwable throwable) {

    super(throwable);
  }
}