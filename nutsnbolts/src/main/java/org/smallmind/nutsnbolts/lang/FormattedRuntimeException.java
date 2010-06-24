package org.smallmind.nutsnbolts.lang;

public class FormattedRuntimeException extends RuntimeException {

   public FormattedRuntimeException () {

      super();
   }

   public FormattedRuntimeException (String message, Object... args) {

      super(String.format(message, args));
   }

   public FormattedRuntimeException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public FormattedRuntimeException (Throwable throwable) {

      super(throwable);
   }
}