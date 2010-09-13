package org.smallmind.wicket.component.google.visualization;

import java.util.Date;
import org.joda.time.DateTime;

public class DateTimeValue extends TimeBasedValue {

   private static DateTimeValue NULL_VALUE = new DateTimeValue(null);

   public static DateTimeValue asNull () {

      return NULL_VALUE;
   }

   public static DateTimeValue create (long milliseconds) {

      return new DateTimeValue(new DateTime(milliseconds));
   }

   public static DateTimeValue create (Long milliseconds) {

      return (milliseconds == null) ? NULL_VALUE : new DateTimeValue(new DateTime(milliseconds));
   }

   public static DateTimeValue create (Date date) {

      return (date == null) ? NULL_VALUE : new DateTimeValue(new DateTime(date));
   }

   private DateTimeValue (DateTime instant) {

      super(instant);
   }

   @Override
   public ValueType getType () {

      return ValueType.DATETIME;
   }

   public int getYear () {

      return getInstant().getYear();
   }

   public int getMonth () {

      return getInstant().getMonthOfYear() - 1;
   }

   public int getDay () {

      return getInstant().getDayOfMonth();
   }

   public int getHour () {

      return getInstant().getHourOfDay();
   }

   public int getMinute () {

      return getInstant().getMinuteOfHour();
   }

   public int getSecond () {

      return getInstant().getSecondOfMinute();
   }

   public int getMillisecond () {

      return getInstant().getMillisOfSecond();
   }

   public String forScript () {

      if (getInstant() == null) {

         return "null";
      }

      StringBuilder dateTimeBuilder = new StringBuilder("new Date(");

      dateTimeBuilder.append(getYear()).append(',').append(getMonth()).append(',').append(getDay()).append(',').append(getHour()).append(',').append(getMinute()).append(',').append(getSecond()).append(',').append(getMillisecond()).append(')');

      return dateTimeBuilder.toString();
   }
}


