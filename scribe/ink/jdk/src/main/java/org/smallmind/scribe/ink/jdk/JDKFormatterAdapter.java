package org.smallmind.scribe.ink.jdk;

import java.util.Collection;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Record;

public class JDKFormatterAdapter implements org.smallmind.scribe.pen.Formatter {

   private Formatter formatter;

   public JDKFormatterAdapter (Formatter formatter) {

      this.formatter = formatter;
   }

   public java.util.logging.Formatter getNativeFormatter () {

      return formatter;
   }

   public String format (Record record, Collection<Filter> filterCollection) {

      StringBuilder formatBuilder = new StringBuilder();
      String header;
      String footer;

      header = formatter.getHead(null);
      if (header != null) {
         formatBuilder.append(header);
      }

      formatBuilder.append(formatter.format((LogRecord)record.getNativeLogEntry()));

      footer = formatter.getTail(null);
      if (footer != null) {
         formatBuilder.append(footer);
      }

      return formatBuilder.toString();
   }
}