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
 * Records the origin of a test failure or error for propagation through the dependency graph.
 * <p>
 * When a lifecycle or test method throws, a {@code Culprit} is created and carried forward so that
 * dependent tests can be skipped with a meaningful message. On construction the throwable chain is
 * walked to locate the first stack frame belonging to the originating test class, producing a compact
 * description. If no matching frame is found the outermost throwable and the explicitly supplied class
 * and method names are used as a fallback.
 */
public class Culprit {

  private final Topmost topmost;
  private final String className;
  private final String methodName;

  /**
   * Creates a culprit for the given failure location.
   * <p>
   * The constructor immediately walks the throwable chain via {@link #findTopmost} to locate the
   * most relevant stack frame. The result is stored for use by {@link #toString()}.
   *
   * @param className  fully qualified name of the class where the failure occurred; must not be {@code null}
   * @param methodName name of the method where the failure occurred; must not be {@code null}
   * @param throwable  throwable to scan for a matching stack frame; must not be {@code null}
   */
  public Culprit (String className, String methodName, Throwable throwable) {

    this.className = className;
    this.methodName = methodName;

    topmost = findTopmost(className, throwable);
  }

  /**
   * Returns a compact description of the failure origin.
   * <p>
   * The format is {@code <class>.<method>:<line> <ExceptionType> <message>}. When a matching
   * stack frame was found its coordinates are used; otherwise the constructor-supplied class and
   * method are used with line {@code -1}.
   *
   * @return human-readable failure origin string; never {@code null}
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
   * Walks the throwable chain to find the first stack frame belonging to the target class.
   * <p>
   * Each throwable's stack trace is searched in order; when a frame whose class name matches
   * {@code className} is found it is paired with its throwable and returned. If the entire chain
   * is exhausted without a match, the root throwable is returned with a {@code null} frame.
   *
   * @param className class name to search for in the stack traces; must not be {@code null}
   * @param throwable root of the throwable chain to scan; must not be {@code null}
   * @return {@link Topmost} pairing the best-matching throwable with its frame; never {@code null}
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
   * Pairs a throwable with the stack frame within it that best identifies the failure origin.
   */
  private class Topmost {

    private final Throwable throwable;
    private final StackTraceElement stackTraceElement;

    /**
     * Constructs a topmost record.
     *
     * @param throwable         throwable that owns the matched frame; must not be {@code null}
     * @param stackTraceElement matching stack frame, or {@code null} if none was found
     */
    public Topmost (Throwable throwable, StackTraceElement stackTraceElement) {

      this.throwable = throwable;
      this.stackTraceElement = stackTraceElement;
    }

    /**
     * @return throwable from which the matched frame was taken; never {@code null}
     */
    public Throwable getThrowable () {

      return throwable;
    }

    /**
     * @return stack frame matching the target class, or {@code null} when none was found
     */
    public StackTraceElement getStackTraceElement () {

      return stackTraceElement;
    }
  }
}
