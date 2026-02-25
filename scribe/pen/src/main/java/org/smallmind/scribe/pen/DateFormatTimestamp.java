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

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Timestamp implementation that formats dates using a {@link DateFormat}.
 */
public class DateFormatTimestamp implements Timestamp {

  private static final DateFormatTimestamp STANDARD_TIMESTAMP = new DateFormatTimestamp();

  private DateTimeFormatter dateTimeFormatter;

  /**
   * Constructs a timestamp using the default ISO-like pattern.
   */
  public DateFormatTimestamp () {

    this(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
  }

  /**
   * Constructs a timestamp using the supplied {@link DateFormat}.
   *
   * @param dateTimeFormatter formatter to use
   */
  public DateFormatTimestamp (DateTimeFormatter dateTimeFormatter) {

    this.dateTimeFormatter = dateTimeFormatter;
  }

  /**
   * Returns the shared default timestamp instance.
   *
   * @return default {@link DateFormatTimestamp}
   */
  public static DateFormatTimestamp getDefaultInstance () {

    return STANDARD_TIMESTAMP;
  }

  /**
   * Retrieves the configured {@link DateFormat}.
   *
   * @return current date formatter
   */
  public DateTimeFormatter getDateTimeFormatter () {

    return dateTimeFormatter;
  }

  /**
   * Sets the {@link DateFormat} used to render timestamps.
   *
   * @param dateTimeFormatter formatter to use
   */
  public void setDateTimeFormatter (DateTimeFormatter dateTimeFormatter) {

    this.dateTimeFormatter = dateTimeFormatter;
  }

  /**
   * Formats the supplied date into a timestamp string.
   *
   * @param date date to format
   * @return formatted timestamp
   * @throws RuntimeException if the underlying {@link DateFormat} fails
   */
  public synchronized String getTimestamp (LocalDateTime date) {

    return dateTimeFormatter.format(date.atZone(ZoneId.systemDefault()));
  }
}
