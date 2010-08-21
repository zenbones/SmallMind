package org.smallmind.nutsnbolts.calendar;

public class CalendarUtilities {

   //   year month[1-12] day[1-31] weekday[1-7](SUNDAY-SATURDAY) hour[0-23] minute[0-59]

   private static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

   public static Month getMonth (int month) {

      return Month.values()[month - 1];
   }

   public static Day getDay (int dayOfWeek) {

      return Day.values()[dayOfWeek - 1];
   }

   public static int getDaysInYear (int year) {

      return ((year % 4) == 0) ? 366 : 365;
   }

   public static int getDaysInMonth (int year, int month) {

      if (month == 2) {
         if ((year % 4) == 0) {
            return 29;
         }
      }

      return DAYS_IN_MONTH[month - 1];
   }

   public static int getDayOfWeek (int year, int month, int day) {

      int weekday;
      int monthArtifact;
      int adjustedYear;
      int adjustedMonth;

      monthArtifact = (14 - month) / 12;
      adjustedYear = year - monthArtifact;
      adjustedMonth = (month - 2) + (12 * monthArtifact);
      weekday = ((day + adjustedYear + (adjustedYear / 4) - (adjustedYear / 100) + (adjustedYear / 400) + (31 * adjustedMonth / 12)) % 7) + 1;

      return weekday;
   }

}
