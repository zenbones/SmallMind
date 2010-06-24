package org.smallmind.scribe.pen;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class LoggerRuntimeException extends FormattedRuntimeException {

   public LoggerRuntimeException () {

      super();
   }

   public LoggerRuntimeException (String message, Object... args) {

      super(message, args);
   }

   public LoggerRuntimeException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public LoggerRuntimeException (Throwable exception) {

      super(exception);
   }
}