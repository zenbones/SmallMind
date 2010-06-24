package org.smallmind.scribe.pen;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class LoggerException extends FormattedException {

   public LoggerException () {

      super();
   }

   public LoggerException (String message, Object... args) {

      super(message, args);
   }

   public LoggerException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public LoggerException (Throwable exception) {

      super(exception);
   }
}