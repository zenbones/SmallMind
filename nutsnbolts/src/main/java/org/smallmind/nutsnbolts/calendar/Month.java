package org.smallmind.nutsnbolts.calendar;

import org.smallmind.nutsnbolts.util.StringUtilities;

public enum Month {

   JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

   public String getDisplayName () {

      return StringUtilities.toDisplayCase(this.name());
   }

}
