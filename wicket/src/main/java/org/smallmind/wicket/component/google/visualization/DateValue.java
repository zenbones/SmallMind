package org.smallmind.wicket.component.google.visualization;

import java.util.Date;
import org.joda.time.DateTime;

public class DateValue extends TimeBasedValue {

   private static DateValue NULL_VALUE = new DateValue(null);

   public static DateValue asNull () {

      return NULL_VALUE;
   }

   public static DateValue create (long milliseconds) {

      return new DateValue(new DateTime(milliseconds));
   }

   public static DateValue create (Long milliseconds) {

      return (milliseconds == null) ? NULL_VALUE : new DateValue(new DateTime(milliseconds));
   }

   public static DateValue create (Date date) {

      return (date == null) ? NULL_VALUE : new DateValue(new DateTime(date));
   }

   private DateValue (DateTime instant) {

      super(instant);
   }

   @Override
   public ValueType getType () {

      return ValueType.DATE;
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

   public String forScript () {

      if (getInstant() == null) {

         return "null";
      }

      StringBuilder dateBuilder = new StringBuilder("new Date(");

      dateBuilder.append(getYear()).append(',').append(getMonth()).append(',').append(getDay()).append(",0,0,0,0)");

      return dateBuilder.toString();
   }
}


