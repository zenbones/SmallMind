package org.smallmind.license;

/**
 * Unchecked wrapper used to smuggle checked exceptions through lambda expressions and stream
 * operations that do not permit declared checked types.
 *
 * <p>Throw a {@code WrappedException} inside the lambda, catch it outside, and call
 * {@link #convert(Class)} to recover the original typed cause.
 */
public class WrappedException extends RuntimeException {

  /**
   * Wraps the given checked exception for deferred, typed recovery.
   *
   * @param exception the checked exception to wrap; must not be {@code null}
   */
  public WrappedException (Exception exception) {

    super(exception);
  }

  /**
   * Casts the wrapped cause to the specified exception type and returns it.
   *
   * <p>The caller is responsible for ensuring that the wrapped exception is actually an instance
   * of {@code exceptionClass}; a {@link ClassCastException} is thrown otherwise.
   *
   * @param exceptionClass the target checked exception class; must not be {@code null}
   * @param <E>            the target exception type
   * @return the wrapped cause cast to {@code E}; never {@code null}
   * @throws ClassCastException if the wrapped cause is not an instance of {@code exceptionClass}
   */
  public <E extends Exception> E convert (Class<E> exceptionClass) {

    return exceptionClass.cast(getCause());
  }
}
