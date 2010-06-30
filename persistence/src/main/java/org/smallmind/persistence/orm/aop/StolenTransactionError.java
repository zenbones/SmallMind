package org.smallmind.persistence.orm.aop;

import org.smallmind.nutsnbolts.lang.FormattedError;

public class StolenTransactionError extends FormattedError {

   public StolenTransactionError () {

      super();
   }

   public StolenTransactionError (String message, Object... args) {

      super(message, args);
   }

   public StolenTransactionError (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public StolenTransactionError (Throwable throwable) {

      super(throwable);
   }
}