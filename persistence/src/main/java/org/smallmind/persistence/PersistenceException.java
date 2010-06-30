package org.smallmind.persistence;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class PersistenceException extends FormattedException {

   public PersistenceException () {

      super();
   }

   public PersistenceException (String message, Object... args) {

      super(message, args);
   }

   public PersistenceException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public PersistenceException (Throwable throwable) {

      super(throwable);
   }
}
