package org.smallmind.quorum.pool;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ConnectionPoolException extends FormattedException {

   public ConnectionPoolException () {

      super();
   }

   public ConnectionPoolException (String message, Object... args) {

      super(message, args);
   }

   public ConnectionPoolException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ConnectionPoolException (Throwable throwable) {

      super(throwable);
   }
}
