package org.smallmind.scribe.pen;

import java.util.Date;

public class NullTimestamp implements Timestamp {

   public String getTimestamp (Date date) {

      return "";
   }
}
