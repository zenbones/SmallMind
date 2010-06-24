package org.smallmind.nutsnbolts.lang;

public class StaleClassLoaderException extends ClassNotFoundException {

   public StaleClassLoaderException () {

      super();
   }

   public StaleClassLoaderException (String message, Object... args) {

      super(String.format(message, args));
   }

   public StaleClassLoaderException (Throwable throwable, String message, Object... args) {

      super(String.format(message, args), throwable);
   }

   public StaleClassLoaderException (Throwable throwable) {

      super();

      initCause(throwable);
   }
}

