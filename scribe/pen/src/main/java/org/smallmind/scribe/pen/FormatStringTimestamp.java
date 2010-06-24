package org.smallmind.scribe.pen;

import java.util.Date;

public class FormatStringTimestamp implements Timestamp {

   private String format;

   public FormatStringTimestamp () {

      this("%tY-%tm-%td");
   }

   public FormatStringTimestamp (String format) {

      this.format = format;
   }

   public String getFormat () {

      return format;
   }

   public void setFormat (String format) {

      this.format = format;
   }

   public String getTimestamp (Date date) {

      return String.format(format, date);
   }
}