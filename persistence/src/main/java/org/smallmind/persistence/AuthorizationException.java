package org.smallmind.persistence;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class AuthorizationException extends FormattedRuntimeException {

   public AuthorizationException () {

      super();
   }

   public AuthorizationException (String message, Object... args) {

      super(message, args);
   }

   public AuthorizationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public AuthorizationException (Throwable throwable) {

      super(throwable);
   }
}