package org.smallmind.persistence.orm.aop;

public class SessionBoundaryError extends SessionError {

   public SessionBoundaryError (int closure) {

      super(closure);
   }

   public SessionBoundaryError (int closure, String message, Object... args) {

      super(closure, message, args);
   }

   public SessionBoundaryError (int closure, Throwable throwable, String message, Object... args) {

      super(closure, throwable, message, args);
   }

   public SessionBoundaryError (int closure, Throwable throwable) {

      super(closure, throwable);
   }
}