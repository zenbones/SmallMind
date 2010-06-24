package org.smallmind.persistence.orm.aop;

public class IncompleteTransactionError extends TransactionError {

   public IncompleteTransactionError (int closure) {

      super(closure);
   }

   public IncompleteTransactionError (int closure, String message, Object... args) {

      super(closure, message, args);
   }

   public IncompleteTransactionError (int closure, Throwable throwable, String message, Object... args) {

      super(closure, throwable, message, args);
   }

   public IncompleteTransactionError (int closure, Throwable throwable) {

      super(closure, throwable);
   }
}
