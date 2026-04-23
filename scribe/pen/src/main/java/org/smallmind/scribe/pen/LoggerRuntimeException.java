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

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

/**
 * An unchecked exception that represents a programming error or unexpected runtime failure within
 * the Scribe logging subsystem, such as attempting to re-initialize a single-init template.
 */
public class LoggerRuntimeException extends FormattedRuntimeException {

  /**
   * Constructs an exception with no message and no cause.
   */
  public LoggerRuntimeException () {

    super();
  }

  /**
   * Constructs an exception with a formatted message and no cause.
   *
   * @param message a format string describing the failure
   * @param args    arguments to substitute into {@code message}
   */
  public LoggerRuntimeException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Constructs an exception with a cause and a formatted message.
   *
   * @param throwable the underlying exception that caused this failure
   * @param message   a format string describing the failure
   * @param args      arguments to substitute into {@code message}
   */
  public LoggerRuntimeException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  /**
   * Constructs an exception with a cause and no additional message.
   *
   * @param exception the underlying exception that caused this failure
   */
  public LoggerRuntimeException (Throwable exception) {

    super(exception);
  }
}
