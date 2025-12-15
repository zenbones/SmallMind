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
 * Convenience helpers for translating between legacy time representations and {@link ZonedDateTime},
 * along with parsing and formatting using ISO-8601 variants.
 */
public class TimeUtility {

  private static final DateTimeFormatter ISO_ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
  private static final DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter ISO_LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  /**
   * Converts epoch milliseconds to a {@link ZonedDateTime} in the system default zone.
   *
   * @param milliseconds epoch milliseconds
   * @return zoned date-time representation
   */
  public static ZonedDateTime fromMilliseconds (long milliseconds) {

    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
  }

  /**
   * Converts boxed epoch milliseconds to a {@link ZonedDateTime}, returning {@code null} when the input is {@code null}.
   *
   * @param milliseconds epoch milliseconds; may be {@code null}
   * @return zoned date-time or {@code null} when input is {@code null}
   */
  public static ZonedDateTime fromMilliseconds (Long milliseconds) {

    return fromMilliseconds(milliseconds, false);
  }

  /**
   * Converts boxed epoch milliseconds to a {@link ZonedDateTime}.
   *
   * @param milliseconds epoch milliseconds; may be {@code null}
   * @param allowNull    when {@code true} returns {@code null} for null input; when {@code false} returns {@link ZonedDateTime#now()}
   * @return zoned date-time representation or {@code null}/now depending on {@code allowNull}
   */
  public static ZonedDateTime fromMilliseconds (Long milliseconds, boolean allowNull) {

    return (milliseconds == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
  }

  /**
   * Converts a {@link Date} to a {@link ZonedDateTime} in the system default zone.
   *
   * @param date legacy date; may be {@code null}
   * @return zoned date-time or {@code null} when input is {@code null}
   */
  public static ZonedDateTime fromDate (Date date) {

    return fromDate(date, false);
  }

  /**
   * Converts a {@link Date} to a {@link ZonedDateTime}.
   *
   * @param date      legacy date; may be {@code null}
   * @param allowNull when {@code true} returns {@code null} for null input; when {@code false} returns {@link ZonedDateTime#now()}
   * @return zoned date-time representation or {@code null}/now depending on {@code allowNull}
   */
  public static ZonedDateTime fromDate (Date date, boolean allowNull) {

    return (date == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  /**
   * Converts a {@link LocalDateTime} to a {@link ZonedDateTime} in the system default zone.
   *
   * @param localDateTime local date-time; may be {@code null}
   * @return zoned date-time or {@code null} when input is {@code null}
   */
  public static ZonedDateTime fromLocalDateTime (LocalDateTime localDateTime) {

    return fromLocalDateTime(localDateTime, false);
  }

  /**
   * Converts a {@link LocalDateTime} to a {@link ZonedDateTime}.
   *
   * @param localDateTime local date-time; may be {@code null}
   * @param allowNull     when {@code true} returns {@code null} for null input; when {@code false} returns {@link ZonedDateTime#now()}
   * @return zoned date-time representation or {@code null}/now depending on {@code allowNull}
   */
  public static ZonedDateTime fromLocalDateTime (LocalDateTime localDateTime, boolean allowNull) {

    return (localDateTime == null) ? allowNull ? null : ZonedDateTime.now() : localDateTime.atZone(ZoneId.systemDefault());
  }

  /**
   * Converts a {@link Calendar} to a {@link ZonedDateTime} in the system default zone.
   *
   * @param calendar calendar instance; may be {@code null}
   * @return zoned date-time or {@code null} when input is {@code null}
   */
  public static ZonedDateTime fromCalendar (Calendar calendar) {

    return fromCalendar(calendar, false);
  }

  /**
   * Converts a {@link Calendar} to a {@link ZonedDateTime}.
   *
   * @param calendar  calendar instance; may be {@code null}
   * @param allowNull when {@code true} returns {@code null} for null input; when {@code false} returns {@link ZonedDateTime#now()}
   * @return zoned date-time representation or {@code null}/now depending on {@code allowNull}
   */
  public static ZonedDateTime fromCalendar (Calendar calendar, boolean allowNull) {

    return (calendar == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
  }

  /**
   * Parses a string using ISO-8601 formats, tolerating missing zone information by assuming the system default zone.
   *
   * @param value ISO-like date or date-time string; may be null
   * @return parsed {@link ZonedDateTime}, or {@code null} when input is {@code null}
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
   * Formats a {@link ZonedDateTime} as an ISO-8601 offset date-time string.
   *
   * @param zonedDateTime date-time to format; may be {@code null}
   * @return formatted string or {@code null} when input is {@code null}
   */
  public static String format (ZonedDateTime zonedDateTime) {

    return (zonedDateTime == null) ? null : ISO_OFFSET_DATE_TIME_FORMATTER.format(zonedDateTime);
  }
}
