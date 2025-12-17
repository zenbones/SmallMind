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
package org.smallmind.sleuth.runner;

/**
 * Represents the originating point of a test failure or error.
 * <p>
 * A {@code Culprit} walks the throwable chain to identify the deepest stack trace element
 * that matches the failing test class and formats a concise description.
 */
public class Culprit {

  private final Topmost topmost;
  private final String className;
  private final String methodName;

  /**
   * Creates a culprit representation for a failure in the given test location.
   *
   * @param className  fully qualified class name of the test
   * @param methodName method name of the failing test or lifecycle method
   * @param throwable  cause to inspect for matching stack frames
   */
  public Culprit (String className, String methodName, Throwable throwable) {

    this.className = className;
    this.methodName = methodName;

    topmost = findTopmost(className, throwable);
  }

  /**
   * @return a compact string with the best matching class, method, line number, and throwable message
   */
  @Override
  public String toString () {

    StringBuilder trimmedBuilder = new StringBuilder();

    trimmedBuilder.append((topmost.getStackTraceElement() == null) ? className : topmost.getStackTraceElement().getClassName());
    trimmedBuilder.append('.');
    trimmedBuilder.append((topmost.getStackTraceElement() == null) ? methodName : topmost.getStackTraceElement().getMethodName());
    trimmedBuilder.append(':');
    trimmedBuilder.append((topmost.getStackTraceElement() == null) ? -1 : topmost.getStackTraceElement().getLineNumber());
    trimmedBuilder.append(" ").append(topmost.getThrowable().getClass().getSimpleName());
    trimmedBuilder.append(" ").append(topmost.getThrowable().getMessage());

    return trimmedBuilder.toString();
  }

  /**
   * Walks the throwable chain to find the first stack frame matching the supplied class.
   *
   * @param className class to search for
   * @param throwable throwable to scan
   * @return {@link Topmost} wrapping the discovered frame or the original throwable when none found
   */
  private Topmost findTopmost (String className, Throwable throwable) {

    Throwable currentThrowable = throwable;

    do {
      for (StackTraceElement stackTraceElement : currentThrowable.getStackTrace()) {
        if (className.equals(stackTraceElement.getClassName())) {

          return new Topmost(currentThrowable, stackTraceElement);
        }
      }
    } while ((currentThrowable = currentThrowable.getCause()) != null);

    return new Topmost(throwable, null);
  }

  /**
   * Holds the earliest stack frame that matches the failing class along with its throwable.
   */
  private class Topmost {

    private final Throwable throwable;
    private final StackTraceElement stackTraceElement;

    /**
     * @param throwable         throwable that owns the matching frame
     * @param stackTraceElement stack trace element that matches the target class; may be {@code null} if none match
     */
    public Topmost (Throwable throwable, StackTraceElement stackTraceElement) {

      this.throwable = throwable;
      this.stackTraceElement = stackTraceElement;
    }

    /**
     * @return throwable that produced the culprit frame
     */
    public Throwable getThrowable () {

      return throwable;
    }

    /**
     * @return matching stack trace element or {@code null} when none were found
     */
    public StackTraceElement getStackTraceElement () {

      return stackTraceElement;
    }
  }
}
