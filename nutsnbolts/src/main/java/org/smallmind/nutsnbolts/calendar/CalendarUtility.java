/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.nutsnbolts.calendar;

public class CalendarUtility {

  //   year month[1-12] day[1-31] weekday[1-7](SUNDAY-SATURDAY) hour[0-23] minute[0-59]

  private static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

  public static Month getMonth (int month) {

    return Month.values()[month - 1];
  }

  public static Day getDay (int dayOfWeek) {

    return Day.values()[dayOfWeek - 1];
  }

  public static int getDaysInYear (int year) {

    return ((year % 4) == 0) ? 366 : 365;
  }

  public static int getDaysInMonth (int year, int month) {

    if (month == 2) {
      if ((year % 4) == 0) {
        return 29;
      }
    }

    return DAYS_IN_MONTH[month - 1];
  }

  public static int getDayOfWeek (int year, int month, int day) {

    int weekday;
    int monthArtifact;
    int adjustedYear;
    int adjustedMonth;

    monthArtifact = (14 - month) / 12;
    adjustedYear = year - monthArtifact;
    adjustedMonth = (month - 2) + (12 * monthArtifact);
    weekday = ((day + adjustedYear + (adjustedYear / 4) - (adjustedYear / 100) + (adjustedYear / 400) + (31 * adjustedMonth / 12)) % 7) + 1;

    return weekday;
  }

}
