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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TimeUtilityTest {

  public void testFromMillisecondsPrimitive () {

    ZonedDateTime result = TimeUtility.fromMilliseconds(0L);

    Assert.assertEquals(result.toInstant().toEpochMilli(), 0L);
  }

  public void testFromMillisecondsBoxedNullReturnsNullWhenAllowed () {

    Assert.assertNull(TimeUtility.fromMilliseconds((Long)null, true));
  }

  public void testFromMillisecondsBoxedNullDefaultReturnsNow () {

    Assert.assertNotNull(TimeUtility.fromMilliseconds((Long)null));
  }

  public void testFromDateNullReturnsNullWhenAllowed () {

    Assert.assertNull(TimeUtility.fromDate(null, true));
  }

  public void testFromDateConverts () {

    Date date = new Date(1735689600000L);
    ZonedDateTime result = TimeUtility.fromDate(date);

    Assert.assertEquals(result.toInstant().toEpochMilli(), 1735689600000L);
  }

  public void testFromLocalDateTimeRetainsMoment () {

    LocalDateTime local = LocalDateTime.of(2026, 5, 28, 10, 30);
    ZonedDateTime result = TimeUtility.fromLocalDateTime(local);

    Assert.assertEquals(result.toLocalDateTime(), local);
  }

  public void testFromCalendarRetainsInstant () {

    Calendar cal = new GregorianCalendar(2026, Calendar.MAY, 28, 10, 30, 0);
    ZonedDateTime result = TimeUtility.fromCalendar(cal);

    Assert.assertEquals(result.toInstant(), cal.toInstant());
  }

  public void testParseNullReturnsNull () {

    Assert.assertNull(TimeUtility.parse(null));
  }

  public void testParseIsoLocalDate () {

    ZonedDateTime result = TimeUtility.parse("2026-05-28");

    Assert.assertEquals(result.toLocalDate().toString(), "2026-05-28");
    Assert.assertEquals(result.getHour(), 0);
  }

  public void testParseIsoLocalDateTime () {

    ZonedDateTime result = TimeUtility.parse("2026-05-28T10:30:00");

    Assert.assertEquals(result.getYear(), 2026);
    Assert.assertEquals(result.getMonthValue(), 5);
    Assert.assertEquals(result.getDayOfMonth(), 28);
    Assert.assertEquals(result.getHour(), 10);
  }

  public void testParseIsoOffsetDateTime () {

    ZonedDateTime result = TimeUtility.parse("2026-05-28T10:30:00Z");

    Assert.assertEquals(result.getHour(), 10);
  }

  public void testParseIsoZonedDateTime () {

    ZonedDateTime result = TimeUtility.parse("2026-05-28T10:30:00+00:00[Europe/London]");

    Assert.assertEquals(result.getZone(), ZoneId.of("Europe/London"));
  }

  public void testFormatNullReturnsNull () {

    Assert.assertNull(TimeUtility.format((LocalDateTime)null));
    Assert.assertNull(TimeUtility.format((ZonedDateTime)null));
  }

  public void testFormatZonedDateTimeProducesIsoOffsetString () {

    ZonedDateTime zdt = ZonedDateTime.of(2026, 5, 28, 10, 30, 0, 0, ZoneId.of("UTC"));
    String formatted = TimeUtility.format(zdt);

    Assert.assertTrue(formatted.startsWith("2026-05-28T10:30:00"));
  }
}
