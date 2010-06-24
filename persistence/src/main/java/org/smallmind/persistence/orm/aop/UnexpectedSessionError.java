package org.smallmind.persistence.orm.aop;

public class UnexpectedSessionError extends SessionError {

   public UnexpectedSessionError (int closure) {

      super(closure);
   }

   public UnexpectedSessionError (int closure, String message, Object... args) {

      super(closure, message, args);
   }

   public UnexpectedSessionError (int closure, Throwable throwable, String message, Object... args) {

      super(closure, throwable, message, args);
   }

   public UnexpectedSessionError (int closure, Throwable throwable) {

      super(closure, throwable);
   }
}