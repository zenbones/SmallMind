package org.smallmind.scribe.pen.aop;

import org.smallmind.scribe.pen.LoggerRuntimeException;

public class AutoLogRuntimeException extends LoggerRuntimeException {

   public AutoLogRuntimeException () {

      super();
   }

   public AutoLogRuntimeException (String message, Object... args) {

      super(message, args);
   }

   public AutoLogRuntimeException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public AutoLogRuntimeException (Throwable exception) {

      super(exception);
   }
}