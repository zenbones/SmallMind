package org.smallmind.nutsnbolts.lang;

public class FormattedError extends Error {

   public FormattedError () {

      super();
   }

   public FormattedError (String message, Object... args) {

      super(String.format(message, args));
   }

   public FormattedError (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public FormattedError (Throwable throwable) {

      super(throwable);
   }
}