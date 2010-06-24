package org.smallmind.quorum.pool;

public class ConnectionCreationException extends ConnectionPoolException {

   public ConnectionCreationException () {

      super();
   }

   public ConnectionCreationException (String message, Object... args) {

      super(message, args);
   }

   public ConnectionCreationException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ConnectionCreationException (Throwable throwable) {

      super(throwable);
   }
}
