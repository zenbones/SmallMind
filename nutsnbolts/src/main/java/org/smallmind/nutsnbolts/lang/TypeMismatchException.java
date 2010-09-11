package org.smallmind.nutsnbolts.lang;

public class TypeMismatchException extends IllegalStateException {

   public TypeMismatchException () {

      super();
   }

   public TypeMismatchException (String message, Object... args) {

      super(String.format(message, args));
   }

   public TypeMismatchException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public TypeMismatchException (Throwable throwable) {

      super(throwable);
   }
}

