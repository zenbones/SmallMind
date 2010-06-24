package org.smallmind.persistence.orm;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

public class ORMInitializationException extends FormattedRuntimeException {

   public ORMInitializationException () {

      super();
   }

   public ORMInitializationException (String message, Object... args) {

      super(message, args);
   }

   public ORMInitializationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ORMInitializationException (Throwable throwable) {

      super(throwable);
   }
}