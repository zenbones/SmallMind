/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

public class TimeUtility {

  private static DateTimeFormatter ISO_ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
  private static DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static DateTimeFormatter ISO_LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  public static ZonedDateTime fromMilliseconds (long milliseconds) {

    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
  }

  public static ZonedDateTime fromMilliseconds (Long milliseconds) {

    return fromMilliseconds(milliseconds, false);
  }

  public static ZonedDateTime fromMilliseconds (Long milliseconds, boolean allowNull) {

    return (milliseconds == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
  }

  public static ZonedDateTime fromDate (Date date) {

    return fromDate(date, false);
  }

  public static ZonedDateTime fromDate (Date date, boolean allowNull) {

    return (date == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  public static ZonedDateTime fromCalendar (Calendar calendar) {

    return fromCalendar(calendar, false);
  }

  public static ZonedDateTime fromCalendar (Calendar calendar, boolean allowNull) {

    return (calendar == null) ? allowNull ? null : ZonedDateTime.now() : ZonedDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
  }

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

        return LocalDate.from(ISO_LOCAL_DATE_FORMATTER.parse(value)).atStartOfDay().atZone(ZoneId.systemDefault());
      } else if (!(hasZ || hasPlusOrMinus)) {
        return LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(value)).atZone(ZoneId.systemDefault());
      } else if (!hasOpenSquareBracket) {
        return ZonedDateTime.from(ISO_OFFSET_DATE_TIME_FORMATTER.parse(value));
      } else {
        return ZonedDateTime.from(ISO_ZONED_DATE_TIME_FORMATTER.parse(value));
      }
    }
  }

  public static String format (ZonedDateTime zonedDateTime) {

    return (zonedDateTime == null) ? null : ISO_OFFSET_DATE_TIME_FORMATTER.format(zonedDateTime);
  }
}
