package org.smallmind.persistence.orm;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class SessionEnforcementException extends FormattedRuntimeException {

   public SessionEnforcementException () {

      super();
   }

   public SessionEnforcementException (String message, Object... args) {

      super(message, args);
   }

   public SessionEnforcementException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public SessionEnforcementException (Throwable throwable) {

      super(throwable);
   }
}
