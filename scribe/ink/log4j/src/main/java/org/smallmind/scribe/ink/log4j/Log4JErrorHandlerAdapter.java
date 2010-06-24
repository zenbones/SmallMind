package org.smallmind.scribe.ink.log4j;

import org.apache.log4j.spi.LoggingEvent;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Record;

public class Log4JErrorHandlerAdapter implements ErrorHandler {

   private org.apache.log4j.spi.ErrorHandler errorHandler;

   public Log4JErrorHandlerAdapter (org.apache.log4j.spi.ErrorHandler errorHandler) {

      this.errorHandler = errorHandler;
   }

   public org.apache.log4j.spi.ErrorHandler getNativeErrorHandler () {

      return errorHandler;
   }

   public void setBackupAppender (Appender appender) {

      errorHandler.setBackupAppender(new Log4JAppenderWrapper(appender));
   }

   public void process (Record record, Exception exception, String errorMessage, Object... args) {

      errorHandler.error(MessageTranslator.translateMessage(errorMessage, args), exception, 0, (LoggingEvent)record.getNativeLogEntry());
   }
}
