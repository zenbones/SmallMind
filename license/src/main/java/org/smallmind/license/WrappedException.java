package org.smallmind.license;

public class WrappedException extends RuntimeException {

  public WrappedException (Exception exception) {

    super(exception);
  }

  public <E extends Exception> E convert (Class<E> exceptionClass) {

    return exceptionClass.cast(getCause());
  }
}