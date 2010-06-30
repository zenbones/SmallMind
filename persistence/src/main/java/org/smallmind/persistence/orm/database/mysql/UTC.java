package org.smallmind.persistence.orm.type;

import java.util.Date;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class UTC {

   private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();

   public static DateTime isoParse (String date) {

      return ISO_DATE_TIME_FORMATTER.parseDateTime(date);
   }

   public static String isoFormat (Date date) {

      return ISO_DATE_TIME_FORMATTER.print(date.getTime());
   }

   public static Date now () {

      DateTime now;

      return (now = new DateTime()).minusMillis(now.getZone().getOffset(now)).toDate();
   }

   public static Date then (Date date) {

      DateTime then;

      return (then = new DateTime(date.getTime())).minusMillis(then.getZone().getOffset(then)).toDate();
   }

   public static Date local (Date date, int offset) {

      return new DateTime(date.getTime()).withZoneRetainFields(DateTimeZone.forOffsetHours(offset)).plusHours(offset).toDate();
   }
}
