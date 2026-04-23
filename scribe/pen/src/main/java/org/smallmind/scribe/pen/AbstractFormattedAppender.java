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
 * Extension of {@link AbstractAppender} that converts log records to formatted strings before
 * writing them to an output target. Subclasses implement {@link #handleOutput(String)} to emit
 * the formatted text; this class bridges the record-based and string-based output contracts.
 */
public abstract class AbstractFormattedAppender extends AbstractAppender implements FormattedAppender {

  private Formatter formatter;

  /**
   * Constructs a formatted appender with no formatter, no error handler, and no name.
   */
  public AbstractFormattedAppender () {

  }

  /**
   * Constructs an unnamed formatted appender with the given formatter and error handler.
   *
   * @param formatter    formatter used to convert records to strings; may be set later via {@link #setFormatter}
   * @param errorHandler handler invoked when output fails; may be {@code null}
   */
  public AbstractFormattedAppender (Formatter formatter, ErrorHandler errorHandler) {

    super(errorHandler);

    this.formatter = formatter;
  }

  /**
   * Constructs a named formatted appender with the given formatter and error handler.
   *
   * @param name         name used to identify this appender; may be {@code null}
   * @param formatter    formatter used to convert records to strings; may be set later via {@link #setFormatter}
   * @param errorHandler handler invoked when output fails; may be {@code null}
   */
  public AbstractFormattedAppender (String name, Formatter formatter, ErrorHandler errorHandler) {

    super(name, errorHandler);

    this.formatter = formatter;
  }

  /**
   * Returns the formatter that converts log records to output strings.
   *
   * @return the configured formatter, or {@code null} if none has been installed
   */
  public Formatter getFormatter () {

    return formatter;
  }

  /**
   * Installs the formatter used to convert log records to output strings.
   *
   * @param formatter formatter to use for all subsequent records; must not be {@code null} when
   *                  records are being published
   */
  public void setFormatter (Formatter formatter) {

    this.formatter = formatter;
  }

  /**
   * Writes the pre-formatted string to the concrete output target.
   *
   * @param output the formatted representation of a log record
   * @throws Exception if an I/O or encoding error occurs while writing
   */
  public abstract void handleOutput (String output)
    throws Exception;

  /**
   * Formats the record to a string via the configured {@link Formatter} and delegates to
   * {@link #handleOutput(String)}; throws {@link LoggerException} if no formatter has been set.
   *
   * @param record the log record to format and emit
   * @throws LoggerException if no formatter is installed on this appender
   * @throws Exception       if formatting or the downstream string output handler fails
   */
  @Override
  public void handleOutput (Record<?> record)
    throws Exception {

    if (formatter != null) {
      handleOutput(formatter.format(record));
    } else {
      throw new LoggerException("No formatter set for log output on this appender(%s)", this.getClass().getCanonicalName());
    }
  }
}
