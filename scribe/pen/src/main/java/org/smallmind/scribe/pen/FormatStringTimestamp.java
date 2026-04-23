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

import java.time.LocalDateTime;

/**
 * A {@link Timestamp} implementation that produces date-time strings by passing a {@link LocalDateTime}
 * directly to {@link String#format(String, Object...)}, using a {@code %t} conversion-letter format
 * string; the default format is {@code "%tY-%tm-%td"} (year-month-day).
 */
public class FormatStringTimestamp implements Timestamp {

  private String format;

  /**
   * Constructs a timestamp using the default date-only format {@code "%tY-%tm-%td"}.
   */
  public FormatStringTimestamp () {

    this("%tY-%tm-%td");
  }

  /**
   * Constructs a timestamp using the given {@link String#format} format string.
   *
   * @param format a format string whose conversion letters reference the {@link LocalDateTime} argument
   */
  public FormatStringTimestamp (String format) {

    this.format = format;
  }

  /**
   * Returns the current {@link String#format} format string.
   *
   * @return the format string used to render timestamps
   */
  public String getFormat () {

    return format;
  }

  /**
   * Replaces the {@link String#format} format string used to render timestamps.
   *
   * @param format the new format string; must be compatible with a {@link LocalDateTime} argument
   */
  public void setFormat (String format) {

    this.format = format;
  }

  /**
   * Formats the given {@link LocalDateTime} by calling {@link String#format(String, Object...)}
   * with the configured format string and {@code date} as the sole argument.
   *
   * @param date the date to format
   * @return the formatted timestamp string
   */
  public String getTimestamp (LocalDateTime date) {

    return String.format(format, date);
  }
}
