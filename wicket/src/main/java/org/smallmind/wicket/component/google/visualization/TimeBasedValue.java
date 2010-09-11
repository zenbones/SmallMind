package org.smallmind.wicket.component.google.visualization;

import org.joda.time.DateTime;
import org.smallmind.nutsnbolts.lang.TypeMismatchException;

public abstract class TimeBasedValue extends Value {

   private DateTime instant;

   public TimeBasedValue (DateTime instant) {

      this.instant = instant;
   }

   public DateTime getInstant () {

      return instant;
   }

   @Override
   public boolean isNull () {

      return (instant == null);
   }

   @Override
   public int compareTo (Value value) {

      if (!getType().equals(value.getType())) {
         throw new TypeMismatchException();
      }

      if (isNull()) {

         return (value.isNull()) ? 0 : -1;
      }
      else if (value.isNull()) {

         return 1;
      }
      else {

         return instant.compareTo((((TimeBasedValue)value).getInstant()));
      }
   }
}


