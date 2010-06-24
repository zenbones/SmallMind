package org.smallmind.nutsnbolts.lang;

public class StaticInitializationError extends FormattedError {

   public StaticInitializationError () {

      super();
   }

   public StaticInitializationError (String message, Object... args) {

      super(message, args);
   }

   public StaticInitializationError (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public StaticInitializationError (Throwable throwable) {

      super(throwable);
   }
}
