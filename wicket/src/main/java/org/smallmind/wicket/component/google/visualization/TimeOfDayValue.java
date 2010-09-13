package org.smallmind.wicket.component.google.visualization;

import java.util.Date;
import org.joda.time.DateTime;

public class TimeOfDayValue extends TimeBasedValue {

   private static TimeOfDayValue NULL_VALUE = new TimeOfDayValue(null);

   public static TimeOfDayValue asNull () {

      return NULL_VALUE;
   }

   public static TimeOfDayValue create (long milliseconds) {

      return new TimeOfDayValue(new DateTime(milliseconds));
   }

   public static TimeOfDayValue create (Long milliseconds) {

      return (milliseconds == null) ? NULL_VALUE : new TimeOfDayValue(new DateTime(milliseconds));
   }

   public static TimeOfDayValue create (Date date) {

      return (date == null) ? NULL_VALUE : new TimeOfDayValue(new DateTime(date));
   }

   private TimeOfDayValue (DateTime instant) {

      super(instant);
   }

   @Override
   public ValueType getType () {

      return ValueType.TIMEOFDAY;
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

      StringBuilder timeOfDayBuilder = new StringBuilder("[");

      timeOfDayBuilder.append(getHour()).append(',').append(getMinute()).append(',').append(getSecond()).append(',').append(getMillisecond()).append(']');

      return timeOfDayBuilder.toString();
   }
}


