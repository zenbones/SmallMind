package org.smallmind.scribe.ink.log4j;

import java.util.Collection;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Formatter;
import org.smallmind.scribe.pen.Record;

public class Log4JFormatterAdapter implements Formatter {

   private Layout layout;

   public Log4JFormatterAdapter (Layout layout) {

      this.layout = layout;
   }

   public String format (Record record, Collection<Filter> filterCollection) {

      StringBuilder formatBuilder = new StringBuilder();
      String header;
      String footer;

      header = layout.getHeader();
      if (header != null) {
         formatBuilder.append(header);
      }

      formatBuilder.append(layout.format((LoggingEvent)record.getNativeLogEntry()));

      footer = layout.getFooter();
      if (footer != null) {
         formatBuilder.append(footer);
      }

      return formatBuilder.toString();
   }
}
