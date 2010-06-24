package org.smallmind.scribe.ink.jdk;

import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class JDKFilterWrapper implements java.util.logging.Filter {

   private Filter filter;

   public JDKFilterWrapper (Filter filter) {

      this.filter = filter;
   }

   public Filter getInnerFilter () {

      return filter;
   }

   public boolean isLoggable (LogRecord record) {

      return filter.willLog(((RecordWrapper)record).getRecord());
   }

   public int hashCode () {

      return filter.hashCode();
   }

   public boolean equals (Object obj) {

      if (obj instanceof JDKFilterWrapper) {
         return filter.equals(((JDKFilterWrapper)obj).getInnerFilter());
      }

      return filter.equals(obj);
   }
}
