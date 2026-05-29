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

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CalendarUtilityTest {

  public void testGetMonthMapsOneBasedNumberToEnum () {

    Assert.assertEquals(CalendarUtility.getMonth(1), Month.values()[0]);
    Assert.assertEquals(CalendarUtility.getMonth(12), Month.values()[11]);
  }

  public void testGetDayMapsOneBasedNumberToEnum () {

    Assert.assertEquals(CalendarUtility.getDay(1), Day.values()[0]);
    Assert.assertEquals(CalendarUtility.getDay(7), Day.values()[6]);
  }

  public void testGetDaysInYearReturns366ForDivisibleBy4 () {

    Assert.assertEquals(CalendarUtility.getDaysInYear(2024), 366);
  }

  public void testGetDaysInYearReturns365ForNonLeap () {

    Assert.assertEquals(CalendarUtility.getDaysInYear(2025), 365);
    Assert.assertEquals(CalendarUtility.getDaysInYear(2023), 365);
  }

  public void testGetDaysInMonthReturnsCanonicalLengths () {

    Assert.assertEquals(CalendarUtility.getDaysInMonth(2025, 1), 31);
    Assert.assertEquals(CalendarUtility.getDaysInMonth(2025, 4), 30);
    Assert.assertEquals(CalendarUtility.getDaysInMonth(2025, 12), 31);
  }

  public void testFebruaryGets29DaysInLeapYear () {

    Assert.assertEquals(CalendarUtility.getDaysInMonth(2024, 2), 29);
  }

  public void testFebruaryGets28DaysInNonLeapYear () {

    Assert.assertEquals(CalendarUtility.getDaysInMonth(2025, 2), 28);
  }

  public void testGetDayOfWeekUsesOneBasedSundayFirstNumbering () {

    Assert.assertEquals(CalendarUtility.getDayOfWeek(2026, 1, 1), 5);
    Assert.assertEquals(CalendarUtility.getDayOfWeek(2026, 5, 28), 5);
    Assert.assertEquals(CalendarUtility.getDayOfWeek(2024, 2, 29), 5);
    Assert.assertEquals(CalendarUtility.getDayOfWeek(2000, 1, 1), 7);
  }
}
