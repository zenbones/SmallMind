package org.smallmind.scribe.pen.probe;

import org.smallmind.scribe.pen.LoggerRuntimeException;

public class ProbeException extends LoggerRuntimeException {

   public ProbeException () {

      super();
   }

   public ProbeException (String message, Object... args) {

      super(message, args);
   }

   public ProbeException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ProbeException (Throwable exception) {

      super(exception);
   }
}
