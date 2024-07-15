/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.swing.calendar;

import java.util.Calendar;
import java.util.Date;

public class CalendarDate implements Comparable<CalendarDate> {

  private final int year;
  private final int month;
  private final int day;

  public CalendarDate (int year, int month, int day) {

    this.year = year;
    this.month = month;
    this.day = day;
  }

  public CalendarDate (Date date) {

    Calendar calendar;

    calendar = Calendar.getInstance();
    calendar.setTime(date);
    this.year = calendar.get(Calendar.YEAR);
    this.month = calendar.get(Calendar.MONTH) + 1;
    this.day = calendar.get(Calendar.DAY_OF_MONTH);
  }

  public int getYear () {

    return year;
  }

  public int getMonth () {

    return month;
  }

  public int getDay () {

    return day;
  }

  public int intValue () {

    return (year * 10000) + (month * 100) + day;
  }

  public boolean before (CalendarDate calendarDate) {

    return intValue() < calendarDate.intValue();
  }

  public boolean beforeOrOn (CalendarDate calendarDate) {

    return intValue() <= calendarDate.intValue();
  }

  public boolean after (CalendarDate calendarDate) {

    return intValue() > calendarDate.intValue();
  }

  public boolean onOrAfter (CalendarDate calendarDate) {

    return intValue() >= calendarDate.intValue();
  }

  public boolean on (CalendarDate calendarDate) {

    return intValue() == calendarDate.intValue();
  }

  public int compareTo (CalendarDate calendarDate) {

    if (this.before(calendarDate)) {
      return -1;
    } else if (this.after(calendarDate)) {
      return 1;
    } else {
      return 0;
    }
  }

  public String toString () {

    return String.valueOf(intValue());
  }

  public int hashCode () {

    return intValue();
  }

  public boolean equals (Object obj) {

    if (obj instanceof CalendarDate) {
      return (intValue() == ((CalendarDate)obj).intValue());
    }

    return false;
  }
}
