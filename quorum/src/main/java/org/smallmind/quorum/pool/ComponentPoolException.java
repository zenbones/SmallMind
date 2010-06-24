package org.smallmind.quorum.pool;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ComponentPoolException extends FormattedException {

   public ComponentPoolException () {

      super();
   }

   public ComponentPoolException (String message, Object... args) {

      super(message, args);
   }

   public ComponentPoolException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ComponentPoolException (Throwable throwable) {

      super(throwable);
   }
}
