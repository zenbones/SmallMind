package org.smallmind.persistence.orm.spring;

import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionUsageException;

public class ProxyTransactionManager implements PlatformTransactionManager {

   private ProxySession proxySession;

   public ProxyTransactionManager (ProxySession proxySession) {

      this.proxySession = proxySession;
   }

   public TransactionStatus getTransaction (TransactionDefinition transactionDefinition)
      throws TransactionException {

      if (transactionDefinition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
         throw new TransactionUsageException("Timeouts are not supported");
      }

      return new ProxyTransactionStatus(proxySession.beginTransaction());
   }

   public void commit (TransactionStatus transactionStatus)
      throws TransactionException {

      ((ProxyTransactionStatus)transactionStatus).getProxyTransaction().commit();
   }

   public void rollback (TransactionStatus transactionStatus)
      throws TransactionException {

      ((ProxyTransactionStatus)transactionStatus).getProxyTransaction().rollback();
   }

   private class ProxyTransactionStatus implements TransactionStatus {

      private ProxyTransaction proxyTransaction;

      public ProxyTransactionStatus (ProxyTransaction proxyTransaction) {

         this.proxyTransaction = proxyTransaction;
      }

      protected ProxyTransaction getProxyTransaction () {

         return proxyTransaction;
      }

      public boolean isNewTransaction () {

         return false;
      }

      public boolean hasSavepoint () {

         return false;
      }

      public void setRollbackOnly () {

         proxyTransaction.setRollbackOnly();
      }

      public boolean isRollbackOnly () {

         return proxyTransaction.isRollbackOnly();
      }

      public boolean isCompleted () {

         return proxyTransaction.isCompleted();
      }

      public Object createSavepoint ()
         throws TransactionException {

         throw new TransactionUsageException("Savepoints are not supported");
      }

      public void rollbackToSavepoint (Object o)
         throws TransactionException {

         throw new TransactionUsageException("Savepoints are not supported");
      }

      public void releaseSavepoint (Object o)
         throws TransactionException {

         throw new TransactionUsageException("Savepoints are not supported");
      }
   }
}
