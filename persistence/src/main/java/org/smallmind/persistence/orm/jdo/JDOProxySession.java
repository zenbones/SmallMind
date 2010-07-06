package org.smallmind.persistence.orm.jdo;

import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.SessionEnforcementException;
import org.smallmind.persistence.orm.aop.BoundarySet;
import org.smallmind.persistence.orm.aop.NonTransactionalState;
import org.smallmind.persistence.orm.aop.RollbackAwareBoundarySet;
import org.smallmind.persistence.orm.aop.TransactionalState;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

public class JDOProxySession extends ProxySession {

   private final ThreadLocal<PersistenceManager> managerThreadLocal = new ThreadLocal<PersistenceManager>();
   private final ThreadLocal<JDOProxyTransaction> transactionThreadLocal = new ThreadLocal<JDOProxyTransaction>();
   private final ThreadLocal<Boolean> boundaryOverrideThreadLocal = new ThreadLocal<Boolean>() {

      protected Boolean initialValue() {

         return false;
      }
   };

   private PersistenceManagerFactory persistenceManagerFactory;

   public JDOProxySession(String dataSourceKey, PersistenceManagerFactory persistenceManagerFactor, boolean enforceBoundary, boolean willCascade) {

      super(dataSourceKey, enforceBoundary, willCascade);

      this.persistenceManagerFactory = persistenceManagerFactor;
   }

   public void setIgnoreBoundaryEnforcement(boolean ignoreBoundaryEnforcement) {

      boundaryOverrideThreadLocal.set(ignoreBoundaryEnforcement);
   }

   public JDOProxyTransaction beginTransaction() {

      JDOProxyTransaction proxyTransaction;
      Transaction transaction;

      if ((proxyTransaction = transactionThreadLocal.get()) == null) {
         if (!(transaction = getPersistenceManager().currentTransaction()).isActive()) {
            transaction.begin();
         }
         proxyTransaction = new JDOProxyTransaction(this, transaction);
         transactionThreadLocal.set(proxyTransaction);
      }

      return proxyTransaction;
   }

   public ProxyTransaction currentTransaction() {

      return transactionThreadLocal.get();
   }

   public void flush() {

      getPersistenceManager().flush();
   }

   public boolean isClosed() {

      PersistenceManager persistenceManager;

      return ((persistenceManager = managerThreadLocal.get()) == null) || (persistenceManager.isClosed());
   }


   public Object getNativeSession() {

      return getPersistenceManager();
   }

   public PersistenceManager getPersistenceManager() {

      PersistenceManager persistenceManager;

      if ((persistenceManager = managerThreadLocal.get()) == null) {
         persistenceManager = persistenceManagerFactory.getPersistenceManager();
         managerThreadLocal.set(persistenceManager);

         RollbackAwareBoundarySet<ProxyTransaction> transactionSet;
         BoundarySet<ProxySession> sessionSet;

         if ((transactionSet = TransactionalState.obtainBoundary(this)) != null) {
            transactionSet.add(beginTransaction());
         } else if ((sessionSet = NonTransactionalState.obtainBoundary(this)) != null) {
            sessionSet.add(this);
         } else if ((!boundaryOverrideThreadLocal.get()) && willEnforceBoundary()) {
            close();
            throw new SessionEnforcementException("Session was requested outside of any boundary enforcement (@NonTransactional or @Transactional)");
         }
      }

      return persistenceManager;
   }

   public void close() {

      PersistenceManager persistenceManager;

      try {
         if ((persistenceManager = managerThreadLocal.get()) != null) {
            persistenceManager.close();
         }
      }
      finally {
         managerThreadLocal.set(null);
         transactionThreadLocal.set(null);
      }
   }
}