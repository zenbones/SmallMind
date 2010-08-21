package org.smallmind.nutsnbolts.calendar;

import org.smallmind.nutsnbolts.util.StringUtilities;

public enum Day {

   SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY;

   public String getDisplayName () {

      return StringUtilities.toDisplayCase(this.name());
   }

}
