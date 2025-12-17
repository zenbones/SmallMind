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
 * Base {@link AbstractAppender} that formats records to strings before output.
 * Subclasses implement {@link #handleOutput(String)} to emit the formatted text.
 */
public abstract class AbstractFormattedAppender extends AbstractAppender implements FormattedAppender {

  private Formatter formatter;

  /**
   * Constructs a formatted appender without formatter or error handler.
   */
  public AbstractFormattedAppender () {

  }

  /**
   * Constructs a formatted appender with a formatter and error handler.
   *
   * @param formatter    formatter to apply
   * @param errorHandler handler to invoke on failures
   */
  public AbstractFormattedAppender (Formatter formatter, ErrorHandler errorHandler) {

    super(errorHandler);

    this.formatter = formatter;
  }

  /**
   * Constructs a formatted appender with name, formatter, and error handler.
   *
   * @param name         appender name
   * @param formatter    formatter to apply
   * @param errorHandler handler to invoke on failures
   */
  public AbstractFormattedAppender (String name, Formatter formatter, ErrorHandler errorHandler) {

    super(name, errorHandler);

    this.formatter = formatter;
  }

  /**
   * Retrieves the formatter used to render records.
   *
   * @return configured formatter, or {@code null} if none set
   */
  public Formatter getFormatter () {

    return formatter;
  }

  /**
   * Sets the formatter used to render records.
   *
   * @param formatter formatter to install
   */
  public void setFormatter (Formatter formatter) {

    this.formatter = formatter;
  }

  /**
   * Emits the formatted string to the concrete output target.
   *
   * @param string formatted record text
   * @throws Exception if writing fails
   */
  public abstract void handleOutput (String string)
    throws Exception;

  /**
   * Formats the record to a string and delegates to the string output handler.
   *
   * @param record record to publish
   * @throws Exception if formatting or output fails
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
