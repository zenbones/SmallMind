package org.smallmind.nutsnbolts.swing.calendar;

import java.util.Calendar;
import java.util.Date;

public class CalendarDate implements Comparable<CalendarDate> {

   private int year;
   private int month;
   private int day;

   public CalendarDate (int year, int month, int day) {

      this.year = year;
      this.month = month;
      this.day = day;
   }

   public CalendarDate (Date date) {

      Calendar calendar;

      calendar = Calendar.getInstance();
      calendar.setTime(date);
      this.year = calendar.get(Calendar.YEAR);
      this.month = calendar.get(Calendar.MONTH) + 1;
      this.day = calendar.get(Calendar.DAY_OF_MONTH);
   }

   public int getYear () {

      return year;
   }

   public int getMonth () {

      return month;
   }

   public int getDay () {

      return day;
   }

   public int intValue () {

      return (year * 10000) + (month * 100) + day;
   }

   public boolean before (CalendarDate calendarDate) {

      return intValue() < calendarDate.intValue();
   }

   public boolean beforeOrOn (CalendarDate calendarDate) {

      return intValue() <= calendarDate.intValue();
   }

   public boolean after (CalendarDate calendarDate) {

      return intValue() > calendarDate.intValue();
   }

   public boolean onOrAfter (CalendarDate calendarDate) {

      return intValue() >= calendarDate.intValue();
   }

   public boolean on (CalendarDate calendarDate) {

      return intValue() == calendarDate.intValue();
   }

   public int compareTo (CalendarDate calendarDate) {

      if (this.before(calendarDate)) {
         return -1;
      }
      else if (this.after(calendarDate)) {
         return 1;
      }
      else {
         return 0;
      }
   }

   public String toString () {

      return String.valueOf(intValue());
   }

   public int hashCode () {

      return intValue();
   }

   public boolean equals (Object obj) {

      if (obj instanceof CalendarDate) {
         return (intValue() == ((CalendarDate)obj).intValue());
      }

      return false;
   }

}
