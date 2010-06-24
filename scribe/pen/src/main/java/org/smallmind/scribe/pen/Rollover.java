package org.smallmind.scribe.pen;

import java.io.File;
import java.util.Date;

public abstract class Rollover {

   private Timestamp timestamp;
   private char separator;

   public Rollover () {

      this('-', DateFormatTimestamp.getDefaultInstance());
   }

   public Rollover (char separator, Timestamp timestamp) {

      this.timestamp = timestamp;
      this.separator = separator;
   }

   public abstract boolean willRollover (File logFile, long bytesToBeWritten);

   public char getSeparator () {

      return separator;
   }

   public void setSeparator (char separator) {

      this.separator = separator;
   }

   public Timestamp getTimestamp () {

      return timestamp;
   }

   public void setTimestamp (Timestamp timestamp) {

      this.timestamp = timestamp;
   }

   public String getTimestampSuffix (Date date) {

      return timestamp.getTimestamp(date);
   }
}