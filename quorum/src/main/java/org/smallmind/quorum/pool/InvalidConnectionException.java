package org.smallmind.quorum.pool;

public class InvalidConnectionException extends ConnectionPoolException {

   public InvalidConnectionException () {

      super();
   }

   public InvalidConnectionException (String message, Object... args) {

      super(message, args);
   }

   public InvalidConnectionException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public InvalidConnectionException (Throwable throwable) {

      super(throwable);
   }
}