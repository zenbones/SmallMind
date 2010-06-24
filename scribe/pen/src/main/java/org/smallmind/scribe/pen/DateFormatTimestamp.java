package org.smallmind.scribe.pen;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatTimestamp implements Timestamp {

   private static final DateFormatTimestamp STANDARD_TIMESTAMP = new DateFormatTimestamp();

   private DateFormat dateFormat;

   public static DateFormatTimestamp getDefaultInstance () {

      return STANDARD_TIMESTAMP;
   }

   public DateFormatTimestamp () {

      this(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
   }

   public DateFormatTimestamp (DateFormat dateFormat) {

      this.dateFormat = dateFormat;
   }

   public DateFormat getDateFormat () {

      return dateFormat;
   }

   public void setDateFormat (DateFormat dateFormat) {

      this.dateFormat = dateFormat;
   }

   public synchronized String getTimestamp (Date date) {

      return dateFormat.format(date);
   }
}
