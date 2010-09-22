package org.smallmind.nutsnbolts.lang;

public class ParserException extends IllegalStateException {

   public ParserException () {

      super();
   }

   public ParserException (String message, Object... args) {

      super(String.format(message, args));
   }

   public ParserException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public ParserException (Throwable throwable) {

      super(throwable);
   }
}

