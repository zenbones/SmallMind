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
 * Callback invoked by appenders and the logging pipeline whenever an internal failure occurs, allowing
 * applications to direct error output to a custom destination instead of the default stack trace.
 */
public interface ErrorHandler {

  /**
   * Handles a logger-level error that is not associated with a specific record.
   *
   * @param loggerName   the name of the logger or appender that encountered the error
   * @param throwable    the exception or error that was caught
   * @param errorMessage a {@link String#format}-style template describing the failure
   * @param args         arguments substituted into {@code errorMessage}
   */
  void process (String loggerName, Throwable throwable, String errorMessage, Object... args);

  /**
   * Handles an error that occurred while the pipeline was processing a specific record.
   *
   * @param record       the record that was being processed when the failure occurred
   * @param throwable    the exception or error that was caught
   * @param errorMessage a {@link String#format}-style template describing the failure
   * @param args         arguments substituted into {@code errorMessage}
   */
  void process (Record<?> record, Throwable throwable, String errorMessage, Object... args);
}
