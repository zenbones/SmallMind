/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.type;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class Time implements Serializable {

  private String timeZoneId;
  private Date epochDate;

  public Time () {

  }

  public static Time fromCalendar (Calendar calendar) {

    return new Time(calendar.getTimeZone().getID(), calendar.getTime());
  }

  public static Time fromDateTime (DateTime dateTime) {

    return new Time(dateTime.getZone().getID(), dateTime.toDate());
  }

  public Time (String timeZoneId, Date epochDate) {

    this.timeZoneId = timeZoneId;
    this.epochDate = epochDate;
  }

  public String getTimeZoneId () {

    return timeZoneId;
  }

  public void setTimeZoneId (String timeZoneId) {

    this.timeZoneId = timeZoneId;
  }

  public Date getEpochDate () {

    return epochDate;
  }

  public void setEpochDate (Date epochDate) {

    this.epochDate = epochDate;
  }

  public Calendar toCalendar () {

    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));

    calendar.setTime(epochDate);

    return calendar;
  }

  public DateTime toDateTime () {

    return new DateTime(epochDate, DateTimeZone.forID(timeZoneId));
  }
}
