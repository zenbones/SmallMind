package org.smallmind.nutsnbolts.lang;

public class FormattedException extends Exception {

   public FormattedException () {

      super();
   }

   public FormattedException (String message, Object... args) {

      super(String.format(message, args));
   }

   public FormattedException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public FormattedException (Throwable throwable) {

      super(throwable);
   }
}