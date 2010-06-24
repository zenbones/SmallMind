package org.smallmind.nutsnbolts.lang;

public class StartupError extends FormattedError {

   public StartupError () {

      super();
   }

   public StartupError (String message, Object... args) {

      super(message, args);
   }

   public StartupError (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public StartupError (Throwable throwable) {

      super(throwable);
   }
}
