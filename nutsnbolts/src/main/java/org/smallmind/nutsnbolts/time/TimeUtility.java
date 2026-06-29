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
package org.smallmind.nutsnbolts.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * Utility methods for converting legacy time types to {@link ZonedDateTime} and for parsing and formatting
 * ISO-8601 date-time strings.
 */
public class TimeUtility {

  private static final DateTimeFormatter ISO_ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
  private static final DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter ISO_LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  /**
   * Converts a primitive epoch-millisecond value to a {@link ZonedDateTime} in the system default zone.
   *
   * @param milliseconds epoch milliseconds since the Unix epoch
   * @return the corresponding {@link ZonedDateTime} in the system default time zone
   */
  public static ZonedDateTime fromMilliseconds (long milliseconds) {

    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
  }

  /**
   * Converts a boxed epoch-millisecond value to a {@link ZonedDateTime}, returning {@code null} when the input
   * is {@code null}.
   *
   * @param milliseconds epoch milliseconds since the Unix epoch, or {@code null}
   * @return the corresponding {@link ZonedDateTime}, or {@code null} if {@code milliseconds} is {@code null}
   */
  public static ZonedDateTime fromMilliseconds (Long milliseconds) {

    return fromMilliseconds(milliseconds, false);
  }

  /**
   * Converts a boxed epoch-millisecond value to a {@link ZonedDateTime}, with configurable null handling.
   *
   * @param milliseconds epoch milliseconds since the Unix epoch, or {@code null}
   * @param allowNull    when {@code true}, a null input returns {@code null}; when {@code false}, a null input returns
   *                     {@link ZonedDateTime#now()}
   * @return the corresponding {@link ZonedDateTime}, or {@code null}/now for a null input based on {@code allowNull}
   */
  public static ZonedDateTime fromMilliseconds (Long milliseconds, boolean allowNull) {

    return (milliseconds == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
  }

  /**
   * Converts a legacy {@link Date} to a {@link ZonedDateTime} in the system default zone, returning {@code null}
   * when the input is {@code null}.
   *
   * @param date the legacy date to convert, or {@code null}
   * @return the corresponding {@link ZonedDateTime}, or {@code null} if {@code date} is {@code null}
   */
  public static ZonedDateTime fromDate (Date date) {

    return fromDate(date, false);
  }

  /**
   * Converts a legacy {@link Date} to a {@link ZonedDateTime} with configurable null handling.
   *
   * @param date      the legacy date to convert, or {@code null}
   * @param allowNull when {@code true}, a null input returns {@code null}; when {@code false}, a null input returns
   *                  {@link ZonedDateTime#now()}
   * @return the corresponding {@link ZonedDateTime}, or {@code null}/now for a null input based on {@code allowNull}
   */
  public static ZonedDateTime fromDate (Date date, boolean allowNull) {

    return (date == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  /**
   * Converts a {@link LocalDateTime} to a {@link ZonedDateTime} in the system default zone, returning {@code null}
   * when the input is {@code null}.
   *
   * @param localDateTime the local date-time to convert, or {@code null}
   * @return the corresponding {@link ZonedDateTime}, or {@code null} if {@code localDateTime} is {@code null}
   */
  public static ZonedDateTime fromLocalDateTime (LocalDateTime localDateTime) {

    return fromLocalDateTime(localDateTime, false);
  }

  /**
   * Converts a {@link LocalDateTime} to a {@link ZonedDateTime} with configurable null handling.
   *
   * @param localDateTime the local date-time to convert, or {@code null}
   * @param allowNull     when {@code true}, a null input returns {@code null}; when {@code false}, a null input returns
   *                      {@link ZonedDateTime#now()}
   * @return the corresponding {@link ZonedDateTime}, or {@code null}/now for a null input based on {@code allowNull}
   */
  public static ZonedDateTime fromLocalDateTime (LocalDateTime localDateTime, boolean allowNull) {

    return (localDateTime == null) ? allowNull ? null : ZonedDateTime.now() : localDateTime.atZone(ZoneId.systemDefault());
  }

  /**
   * Converts a {@link Calendar} to a {@link ZonedDateTime} in the system default zone, returning {@code null}
   * when the input is {@code null}.
   *
   * @param calendar the calendar instance to convert, or {@code null}
   * @return the corresponding {@link ZonedDateTime}, or {@code null} if {@code calendar} is {@code null}
   */
  public static ZonedDateTime fromCalendar (Calendar calendar) {

    return fromCalendar(calendar, false);
  }

  /**
   * Converts a {@link Calendar} to a {@link ZonedDateTime} with configurable null handling.
   *
   * @param calendar  the calendar instance to convert, or {@code null}
   * @param allowNull when {@code true}, a null input returns {@code null}; when {@code false}, a null input returns
   *                  {@link ZonedDateTime#now()}
   * @return the corresponding {@link ZonedDateTime}, or {@code null}/now for a null input based on {@code allowNull}
   */
  public static ZonedDateTime fromCalendar (Calendar calendar, boolean allowNull) {

    return (calendar == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
  }

  /**
   * Parses an ISO-8601 date or date-time string into a {@link ZonedDateTime}, applying the system default zone when
   * no zone or offset information is present in the input.
   * Handles ISO local date ({@code yyyy-MM-dd}), local date-time, offset date-time, and fully zoned date-time formats.
   *
   * @param value the ISO-8601 string to parse, or {@code null}
   * @return the parsed {@link ZonedDateTime}, or {@code null} if {@code value} is {@code null}
   */
  public static ZonedDateTime parse (String value) {

    if (value == null) {

      return null;
    } else {

      boolean hasT = false;
      boolean hasZ = false;
      boolean hasPlusOrMinus = false;
      boolean hasOpenSquareBracket = false;

      for (int index = 0; index < value.length(); index++) {
        switch (value.charAt(index)) {
          case 'T':
            hasT = true;
            break;
          case 'Z':
            hasZ = true;
            break;
          case '+':
            hasPlusOrMinus = true;
            break;
          case '-':
            // if we're past the 'T' time separator
            hasPlusOrMinus = hasT;
            break;
          case '[':
            hasOpenSquareBracket = true;
            break;
        }
      }

      if (!hasT) {
        return LocalDate.from(ISO_LOCAL_DATE_FORMATTER.parse(value)).atStartOfDay(ZoneId.systemDefault());
      } else if (!(hasZ || hasPlusOrMinus)) {
        return LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(value)).atZone(ZoneId.systemDefault());
      } else if (!hasOpenSquareBracket) {
        return ZonedDateTime.from(ISO_OFFSET_DATE_TIME_FORMATTER.parse(value));
      } else {
        return ZonedDateTime.from(ISO_ZONED_DATE_TIME_FORMATTER.parse(value));
      }
    }
  }

  /**
   * Formats a {@link LocalDateTime} as an ISO-8601 offset date-time string with system default time-zone.
   *
   * @param localDateTime the local date-time to format, or {@code null}
   * @return the ISO-8601 offset date-time string in the system default zone, or {@code null} if {@code localDateTime} is {@code null}
   */
  public static String format (LocalDateTime localDateTime) {

    return (localDateTime == null) ? null : ISO_OFFSET_DATE_TIME_FORMATTER.format(localDateTime.atZone(ZoneId.systemDefault()));
  }

  /**
   * Formats a {@link ZonedDateTime} as an ISO-8601 offset date-time string.
   *
   * @param zonedDateTime the zoned date-time to format, or {@code null}
   * @return the ISO-8601 offset date-time string, or {@code null} if {@code zonedDateTime} is {@code null}
   */
  public static String format (ZonedDateTime zonedDateTime) {

    return (zonedDateTime == null) ? null : ISO_OFFSET_DATE_TIME_FORMATTER.format(zonedDateTime);
  }
}
