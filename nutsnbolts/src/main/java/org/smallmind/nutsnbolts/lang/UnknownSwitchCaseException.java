package org.smallmind.nutsnbolts.lang;

public class UnknownSwitchCaseException extends IllegalStateException {

   public UnknownSwitchCaseException () {

      super();
   }

   public UnknownSwitchCaseException (String message, Object... args) {

      super(String.format(message, args));
   }

   public UnknownSwitchCaseException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public UnknownSwitchCaseException (Throwable throwable) {

      super(throwable);
   }
}

