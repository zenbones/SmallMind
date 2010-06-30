package org.smallmind.persistence.orm.jdo;

import javax.jdo.Transaction;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcessException;

public class JDOProxyTransaction extends ProxyTransaction {

   private Transaction transaction;
   private boolean rolledBack = false;

   public JDOProxyTransaction (JDOProxySession proxySession, Transaction transaction) {

      super(proxySession);

      this.transaction = transaction;
   }

   public boolean isCompleted () {

      return !transaction.isActive();
   }

   public void flush () {

      getSession().flush();
   }

   public void commit () {

      if (isRollbackOnly()) {

         ProxyTransactionException proxyTransactionException = new ProxyTransactionException("Transaction has been set to allow rollback only");

         try {
            rollback();
         }
         catch (Exception exception) {
            proxyTransactionException.initCause(exception);
         }

         throw proxyTransactionException;
      }

      Throwable unexpectedThrowable = null;

      try {
         getSession().flush();
         transaction.commit();
      }
      catch (Throwable throwable) {
         unexpectedThrowable = throwable;
      }
      finally {
         getSession().close();

         try {
            applyPostProcesses((unexpectedThrowable == null) ? TransactionEndState.COMMIT : TransactionEndState.ROLLBACK);
         }
         catch (TransactionPostProcessException transactionPostProcessException) {
            if (unexpectedThrowable != null) {
               transactionPostProcessException.initCause(unexpectedThrowable);
            }

            throw new ProxyTransactionException(transactionPostProcessException);
         }

         if (unexpectedThrowable != null) {
            throw new ProxyTransactionException(unexpectedThrowable);
         }
      }
   }

   public void rollback () {

      if (!rolledBack) {
         try {
            transaction.rollback();
            rolledBack = true;
         }
         finally {
            getSession().close();

            try {
               applyPostProcesses(TransactionEndState.ROLLBACK);
            }
            catch (TransactionPostProcessException transactionPostProcessException) {
               throw new ProxyTransactionException(transactionPostProcessException);
            }
         }
      }
   }
}
