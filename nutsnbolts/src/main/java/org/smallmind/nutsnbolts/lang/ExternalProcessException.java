package org.smallmind.nutsnbolts.lang;

public class ExternalProcessException extends FormattedException {

   public ExternalProcessException () {

      super();
   }

   public ExternalProcessException (String message, Object... args) {

      super(message, args);
   }

   public ExternalProcessException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ExternalProcessException (Throwable throwable) {

      super(throwable);
   }
}
