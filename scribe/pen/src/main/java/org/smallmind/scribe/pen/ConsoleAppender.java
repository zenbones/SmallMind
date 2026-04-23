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

/**
 * Appender that writes formatted log output directly to {@code System.out}, synchronizing each write
 * to prevent interleaved output from concurrent threads.
 */
public class ConsoleAppender extends AbstractFormattedAppender {

  /**
   * Constructs a console appender with no formatter or error handler; the record is written
   * as-is and errors are silently discarded.
   */
  public ConsoleAppender () {

    this(null, null);
  }

  /**
   * Constructs a console appender that applies the given formatter before writing to {@code System.out}.
   *
   * @param formatter the formatter used to convert log records to strings, or {@code null} for no formatting
   */
  public ConsoleAppender (Formatter formatter) {

    this(formatter, null);
  }

  /**
   * Constructs a console appender with a formatter and an error handler.
   *
   * @param formatter    the formatter used to convert log records to strings, or {@code null} for no formatting
   * @param errorHandler the handler invoked when output or formatting fails, or {@code null} to discard errors
   */
  public ConsoleAppender (Formatter formatter, ErrorHandler errorHandler) {

    super(formatter, errorHandler);
  }

  /**
   * Writes the formatted text to {@code System.out}. This method is synchronized so that concurrent
   * callers do not interleave their output.
   *
   * @param formattedOutput the fully formatted text to print; must not be {@code null}
   */
  public synchronized void handleOutput (String formattedOutput) {

    System.out.print(formattedOutput);
  }
}
