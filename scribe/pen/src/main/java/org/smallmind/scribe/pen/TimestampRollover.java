package org.smallmind.scribe.pen;

import java.io.File;
import java.util.Calendar;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class TimestampRollover extends Rollover {

   private TimestampQuantifier timestampQuantifier;

   public TimestampRollover () {

      this(TimestampQuantifier.TOP_OF_DAY);
   }

   public TimestampRollover (TimestampQuantifier timestampQuantifier) {

      super();

      this.timestampQuantifier = timestampQuantifier;
   }

   public TimestampRollover (TimestampQuantifier timestampQuantifier, char separator, Timestamp timestamp) {

      super(separator, timestamp);

      this.timestampQuantifier = timestampQuantifier;
   }

   public TimestampQuantifier getTimestampQuantifier () {

      return timestampQuantifier;
   }

   public void setTimestampQuantifier (TimestampQuantifier timestampQuantifier) {

      this.timestampQuantifier = timestampQuantifier;
   }

   public boolean willRollover (File logFile, long bytesToBeWritten) {

      Calendar now = Calendar.getInstance();
      Calendar lastMod = Calendar.getInstance();

      lastMod.setTimeInMillis(logFile.lastModified());
      switch (timestampQuantifier) {
         case TOP_OF_MINUTE:
            return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH)) || (now.get(Calendar.HOUR_OF_DAY) != lastMod.get(Calendar.HOUR_OF_DAY)) || (now.get(Calendar.MINUTE) != lastMod.get(Calendar.MINUTE));
         case TOP_OF_HOUR:
            return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH)) || (now.get(Calendar.HOUR_OF_DAY) != lastMod.get(Calendar.HOUR_OF_DAY));
         case HALF_DAY:
            return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH)) || (now.get(Calendar.AM_PM) != lastMod.get(Calendar.AM_PM));
         case TOP_OF_DAY:
            return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.DAY_OF_MONTH) != lastMod.get(Calendar.DAY_OF_MONTH));
         case TOP_OF_WEEK:
            return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH)) || (now.get(Calendar.WEEK_OF_MONTH) != lastMod.get(Calendar.WEEK_OF_MONTH));
         case TOP_OF_MONTH:
            return (now.get(Calendar.YEAR) != lastMod.get(Calendar.YEAR)) || (now.get(Calendar.MONTH) != lastMod.get(Calendar.MONTH));
         default:
            throw new UnknownSwitchCaseException(timestampQuantifier.name());
      }
   }
}