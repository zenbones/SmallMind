package org.smallmind.scribe.ink.jdk;

import java.util.logging.ErrorManager;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Record;

public class JDKErrorHandlerAdapter implements ErrorHandler {

   private ErrorManager errorManager;

   public JDKErrorHandlerAdapter (ErrorManager errorManager) {

      this.errorManager = errorManager;
   }

   public ErrorManager getNativeErrorManager () {

      return errorManager;
   }

   public void setBackupAppender (Appender appender) {

      throw new UnsupportedOperationException("Method is not supported by native JDK Logging");
   }

   public void process (Record record, Exception exception, String errorMessage, Object... args) {

      errorManager.error(MessageTranslator.translateMessage(errorMessage, args), exception, 0);
   }
}