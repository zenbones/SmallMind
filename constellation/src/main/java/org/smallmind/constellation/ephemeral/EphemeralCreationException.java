package org.smallmind.constellation.ephemeral;

public class EphemeralCreationException extends EphemeralException {

   public EphemeralCreationException () {

      super();
   }

   public EphemeralCreationException (String message, Object... args) {

      super(message, args);
   }

   public EphemeralCreationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public EphemeralCreationException (Throwable throwable) {

      super(throwable);
   }
}
