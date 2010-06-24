package org.smallmind.persistence.orm.jdo;

import javax.jdo.Transaction;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;

public class JDOProxyTransaction implements ProxyTransaction {

   private JDOProxySession proxySession;
   private Transaction transaction;
   private boolean rolledBack = false;

   public JDOProxyTransaction (JDOProxySession proxySession, Transaction transaction) {

      this.proxySession = proxySession;
      this.transaction = transaction;
   }

   public ProxySession getSession () {

      return proxySession;
   }

   public synchronized boolean isCompleted () {

      return !transaction.isActive();
   }

   public synchronized void setRollbackOnly () {

      transaction.setRollbackOnly();
   }

   public synchronized boolean isRollbackOnly () {

      return transaction.getRollbackOnly();
   }

   public synchronized void commit () {

      commit(false);
   }

   public synchronized void commit (boolean restart) {
      boolean restarted = false;

      if (transaction.getRollbackOnly()) {

         ProxyTransactionException proxyTransactionException = new ProxyTransactionException("Transaction has been set to allow rollback only");

         try {
            rollback();
         }
         catch (Exception exception) {
            proxyTransactionException.initCause(exception);
         }

         throw proxyTransactionException;
      }

      try {
         proxySession.flush();
         transaction.commit();

         if (restart) {
            transaction.begin();
            restarted = true;
         }
      }
      finally {
         if (!restarted) {
            proxySession.close();
         }
      }
   }

   public synchronized void rollback () {

      if (!rolledBack) {
         try {
            transaction.rollback();
            rolledBack = true;
         }
         finally {
            proxySession.close();
         }
      }
   }
}
