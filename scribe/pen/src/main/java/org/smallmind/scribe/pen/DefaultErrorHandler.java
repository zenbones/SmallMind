package org.smallmind.scribe.pen;

import org.smallmind.scribe.pen.adapter.LoggingBlueprintsFactory;

public class DefaultErrorHandler implements ErrorHandler {

   private Appender appender;

   public DefaultErrorHandler () {

      appender = new ConsoleAppender(new XMLFormatter());
   }

   public DefaultErrorHandler (Appender appender) {

      setBackupAppender(appender);
   }

   public void setBackupAppender (Appender appender) {

      this.appender = appender;
   }

   public void process (Record record, Exception exception, String errorMessage, Object... args) {

      appender.publish(LoggingBlueprintsFactory.getLoggingBlueprints().errorRecord(record, exception, errorMessage, args));
      appender.publish(record);
   }
}