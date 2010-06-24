package org.smallmind.scribe.ink.jdk;

import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Record;

public class JDKFilterAdapter implements Filter {

   private java.util.logging.Filter filter;

   public JDKFilterAdapter (java.util.logging.Filter filter) {

      this.filter = filter;
   }

   protected java.util.logging.Filter getNativeFilter () {

      return filter;
   }

   public boolean willLog (Record record) {

      return filter.isLoggable((LogRecord)record.getNativeLogEntry());
   }

   public int hashCode () {

      return filter.hashCode();
   }

   public boolean equals (Object obj) {

      if (obj instanceof JDKFilterAdapter) {
         return filter.equals(((JDKFilterAdapter)obj).getNativeFilter());
      }

      return filter.equals(obj);
   }
}