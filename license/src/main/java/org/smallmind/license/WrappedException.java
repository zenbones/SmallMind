package org.smallmind.license;

/**
 * Runtime wrapper used to tunnel checked exceptions through lambda boundaries and recover the original type later.
 */
public class WrappedException extends RuntimeException {

  /**
   * Wraps the supplied exception for deferred handling.
   *
   * @param exception the exception to wrap
   */
  public WrappedException (Exception exception) {

    super(exception);
  }

  /**
   * Converts the wrapped exception back to the desired checked type.
   *
   * @param exceptionClass the checked exception class to cast to
   * @param <E>            the checked exception type
   * @return the wrapped exception cast to the requested type
   */
  public <E extends Exception> E convert (Class<E> exceptionClass) {

    return exceptionClass.cast(getCause());
  }
}
