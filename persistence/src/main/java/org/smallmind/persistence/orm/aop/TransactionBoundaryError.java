package org.smallmind.persistence.orm.aop;

public class TransactionBoundaryError extends TransactionError {

   public TransactionBoundaryError (int closure) {

      super(closure);
   }

   public TransactionBoundaryError (int closure, String message, Object... args) {

      super(closure, message, args);
   }

   public TransactionBoundaryError (int closure, Throwable throwable, String message, Object... args) {

      super(closure, throwable, message, args);
   }

   public TransactionBoundaryError (int closure, Throwable throwable) {

      super(closure, throwable);
   }
}