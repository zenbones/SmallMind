package org.smallmind.persistence.orm.type;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.terracotta.modules.annotations.InstrumentedClass;

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
