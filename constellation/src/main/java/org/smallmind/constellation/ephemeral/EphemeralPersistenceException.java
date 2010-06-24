package org.smallmind.constellation.ephemeral;

public class EphemeralPersistenceException extends EphemeralException {

   public EphemeralPersistenceException () {

      super();
   }

   public EphemeralPersistenceException (String message, Object... args) {

      super(message, args);
   }

   public EphemeralPersistenceException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public EphemeralPersistenceException (Throwable throwable) {

      super(throwable);
   }
}
