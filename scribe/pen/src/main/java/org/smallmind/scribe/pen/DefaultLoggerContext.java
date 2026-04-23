/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link LoggerContext} that identifies the originating caller by walking
 * {@code Thread.currentThread().getStackTrace()}, skipping frames that belong to the logging
 * infrastructure, and capturing the first non-logging frame; the result is stored under
 * double-checked locking so the stack walk happens at most once per instance.
 */
public class DefaultLoggerContext implements LoggerContext {

  private final AtomicBoolean filled = new AtomicBoolean(false);
  private StackTraceElement contextElement;

  /**
   * Returns {@code true} if the given class name is part of the logging infrastructure as
   * determined by {@link LoggerManager#isLoggingClass(String)}.
   *
   * @param className the fully-qualified class name to test
   * @return {@code true} if the class belongs to the logging stack; {@code false} otherwise
   */
  private static boolean willPrime (String className) {

    return LoggerManager.isLoggingClass(className);
  }

  /**
   * Returns {@code true} if the caller context has already been captured from the stack.
   *
   * @return {@code true} if {@link #setContextElement()} has successfully completed
   */
  public boolean isFilled () {

    return filled.get();
  }

  /**
   * Eagerly captures the caller context from the current thread's stack if not already filled;
   * subsequent calls are no-ops.
   *
   * @throws IllegalStateException if the logging call context cannot be found in the stack
   */
  public void fillIn () {

    setContextElement();
  }

  /**
   * Returns the fully-qualified class name of the first non-logging frame in the call stack.
   *
   * @return the originating class name
   * @throws IllegalStateException if the logging call context cannot be determined
   */
  public String getClassName () {

    setContextElement();

    return contextElement.getClassName();
  }

  /**
   * Returns the method name of the first non-logging frame in the call stack.
   *
   * @return the originating method name
   * @throws IllegalStateException if the logging call context cannot be determined
   */
  public String getMethodName () {

    setContextElement();

    return contextElement.getMethodName();
  }

  /**
   * Returns the source file name of the first non-logging frame in the call stack.
   *
   * @return the originating source file name, or {@code null} if unavailable
   * @throws IllegalStateException if the logging call context cannot be determined
   */
  public String getFileName () {

    setContextElement();

    return contextElement.getFileName();
  }

  /**
   * Returns whether the first non-logging frame is a native method.
   *
   * @return {@code true} if the originating frame is native
   * @throws IllegalStateException if the logging call context cannot be determined
   */
  public boolean isNativeMethod () {

    setContextElement();

    return contextElement.isNativeMethod();
  }

  /**
   * Returns the source line number of the first non-logging frame in the call stack.
   *
   * @return the originating line number, or a negative value if unavailable
   * @throws IllegalStateException if the logging call context cannot be determined
   */
  public int getLineNumber () {

    setContextElement();

    return contextElement.getLineNumber();
  }

  /**
   * Performs a double-checked lock to ensure the stack walk occurs exactly once: iterates over
   * the current thread's stack frames, primes on the first logging-infrastructure frame, then
   * captures the first frame that is no longer part of the infrastructure as the caller context.
   *
   * @throws IllegalStateException if no logging frame is found in the stack, or if no
   *                               non-logging frame follows the logging frames
   */
  public void setContextElement () {

    if (!filled.get()) {
      synchronized (this) {
        if (!filled.get()) {

          boolean primed = false;

          for (StackTraceElement currentElement : Thread.currentThread().getStackTrace()) {
            if (primed) {
              if (!willPrime(currentElement.getClassName())) {
                contextElement = currentElement;
                break;
              }
            } else {
              primed = willPrime(currentElement.getClassName());
            }
          }

          if (!primed || (contextElement == null)) {
            throw new IllegalStateException("The logging call context was not found");
          }

          filled.set(true);
        }
      }
    }
  }
}
