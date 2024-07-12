/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class Culprit {

  private final Topmost topmost;
  private final String className;
  private final String methodName;

  public Culprit (String className, String methodName, Throwable throwable) {

    this.className = className;
    this.methodName = methodName;

    topmost = findTopmost(className, throwable);
  }

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

  private class Topmost {

    private final Throwable throwable;
    private final StackTraceElement stackTraceElement;

    public Topmost (Throwable throwable, StackTraceElement stackTraceElement) {

      this.throwable = throwable;
      this.stackTraceElement = stackTraceElement;
    }

    public Throwable getThrowable () {

      return throwable;
    }

    public StackTraceElement getStackTraceElement () {

      return stackTraceElement;
    }
  }
}
