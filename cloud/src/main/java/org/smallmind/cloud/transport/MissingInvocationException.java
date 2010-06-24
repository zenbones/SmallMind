package org.smallmind.cloud.transport;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class MissingInvocationException extends FormattedException {

   public MissingInvocationException () {

      super();
   }

   public MissingInvocationException (String message, Object... args) {

      super(message, args);
   }

   public MissingInvocationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public MissingInvocationException (Throwable throwable) {

      super(throwable);
   }
}
