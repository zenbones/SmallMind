package org.smallmind.constellation.ephemeral;

public class EphemeralRecoveryException extends EphemeralException {

   public EphemeralRecoveryException () {

      super();
   }

   public EphemeralRecoveryException (String message, Object... args) {

      super(message, args);
   }

   public EphemeralRecoveryException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public EphemeralRecoveryException (Throwable throwable) {

      super(throwable);
   }
}
