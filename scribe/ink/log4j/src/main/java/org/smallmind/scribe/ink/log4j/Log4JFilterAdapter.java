package org.smallmind.scribe.ink.log4j;

import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Record;

public class Log4JFilterAdapter implements Filter {

   private org.apache.log4j.spi.Filter filter;

   public Log4JFilterAdapter (org.apache.log4j.spi.Filter filter) {

      this.filter = filter;
   }

   protected org.apache.log4j.spi.Filter getNativeFilter () {

      return filter;
   }

   public boolean willLog (Record record) {

      return filter.decide((LoggingEvent)record.getNativeLogEntry()) != org.apache.log4j.spi.Filter.DENY;
   }

   public int hashCode () {

      return filter.hashCode();
   }

   public boolean equals (Object obj) {

      if (obj instanceof Log4JFilterAdapter) {
         return filter.equals(((Log4JFilterAdapter)obj).getNativeFilter());
      }

      return filter.equals(obj);
   }
}
