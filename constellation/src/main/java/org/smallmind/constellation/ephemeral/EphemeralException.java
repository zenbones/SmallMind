package org.smallmind.constellation.ephemeral;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class EphemeralException extends FormattedException {

   public EphemeralException () {

      super();
   }

   public EphemeralException (String message, Object... args) {

      super(message, args);
   }

   public EphemeralException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public EphemeralException (Throwable throwable) {

      super(throwable);
   }
}
