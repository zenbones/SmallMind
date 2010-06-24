package org.smallmind.scribe.ink.log4j;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class Log4JAppenderWrapper implements org.apache.log4j.Appender {

   private Appender appender;

   public Log4JAppenderWrapper (Appender appender) {

      this.appender = appender;
   }

   protected Appender getInnerAppender () {

      return appender;
   }

   public String getName () {

      return appender.getName();
   }

   public void setName (String name) {

      appender.setName(name);
   }

   public void addFilter (Filter filter) {

      appender.addFilter(new Log4JFilterAdapter(filter));
   }

   public Filter getFilter () {

      throw new UnsupportedOperationException("Unsupported native Log4J method");
   }

   public void clearFilters () {

      throw new UnsupportedOperationException("Unsupported native Log4J method");
   }

   public void doAppend (LoggingEvent loggingEvent) {

      appender.publish(((RecordWrapper)loggingEvent).getRecord());
   }

   public void close () {

      // Log4J will close all Appenders when removing them from a Logger, even though under Log4J
      // Appenders are shared objects. So we can't let Log4J handle the close method> Bad Log4J.
   }

   public void setErrorHandler (ErrorHandler errorHandler) {

      appender.setErrorHandler(new Log4JErrorHandlerAdapter(errorHandler));
   }

   public ErrorHandler getErrorHandler () {

      throw new UnsupportedOperationException("Unsupported native Log4J method");
   }

   public void setLayout (Layout layout) {

      appender.setFormatter(new Log4JFormatterAdapter(layout));
   }

   public Layout getLayout () {

      throw new UnsupportedOperationException("Unsupported native Log4J method");
   }

   public boolean requiresLayout () {

      return appender.requiresFormatter();
   }

   public int hashCode () {

      return appender.hashCode();
   }

   public boolean equals (Object obj) {

      if (obj instanceof Log4JAppenderWrapper) {
         return appender.equals(((Log4JAppenderWrapper)obj).getInnerAppender());
      }

      return appender.equals(obj);
   }

   protected void finalize () {

      close();
   }
}