package org.smallmind.persistence.orm.jdo;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.SessionEnforcementException;
import org.smallmind.persistence.orm.aop.NonTransactionalState;
import org.smallmind.persistence.orm.aop.TransactionalState;

public class JDOProxySession extends ProxySession {

   private final ThreadLocal<PersistenceManager> managerThreadLocal;
   private final ThreadLocal<JDOProxyTransaction> transactionThreadLocal;
   private PersistenceManagerFactory persistenceManagerFactory;

   public JDOProxySession (PersistenceManagerFactory persistenceManagerFactory) {

      this(null, persistenceManagerFactory, false);
   }

   public JDOProxySession (PersistenceManagerFactory persistenceManagerFactory, boolean enforceBoundary) {

      this(null, persistenceManagerFactory, enforceBoundary);
   }

   public JDOProxySession (String dataSourceKey, PersistenceManagerFactory persistenceManagerFactory) {

      this(dataSourceKey, persistenceManagerFactory, false);
   }

   public JDOProxySession (String dataSourceKey, PersistenceManagerFactory persistenceManagerFactory, boolean enforceBoundary) {

      super(dataSourceKey, enforceBoundary);

      this.persistenceManagerFactory = persistenceManagerFactory;

      managerThreadLocal = new ThreadLocal<PersistenceManager>();
      transactionThreadLocal = new ThreadLocal<JDOProxyTransaction>();
   }

   public JDOProxyTransaction beginTransaction () {

      JDOProxyTransaction proxyTransaction;
      Transaction transaction;

      if ((proxyTransaction = transactionThreadLocal.get()) == null) {
         if (!(transaction = getPersistenceManager(true).currentTransaction()).isActive()) {
            transaction.begin();
         }
         proxyTransaction = new JDOProxyTransaction(this, transaction);
         transactionThreadLocal.set(proxyTransaction);
      }

      return proxyTransaction;
   }

   public ProxyTransaction currentTransaction () {

      return transactionThreadLocal.get();
   }

   public void flush () {

      getPersistenceManager().flush();
   }

   public boolean isClosed () {

      PersistenceManager persistenceManager;

      return ((persistenceManager = managerThreadLocal.get()) == null) || (persistenceManager.isClosed());
   }

   public PersistenceManager getPersistenceManager () {

      return getPersistenceManager(false);
   }

   private PersistenceManager getPersistenceManager (boolean internal) {

      PersistenceManager persistenceManager;
      boolean sessionRequired = false;
      boolean sessionObtained = false;
      boolean transactionObtained = false;

      if ((persistenceManager = managerThreadLocal.get()) == null) {
         persistenceManager = persistenceManagerFactory.getPersistenceManager();
         managerThreadLocal.set(persistenceManager);

         sessionRequired = true;
         if (!internal) {
            sessionObtained = NonTransactionalState.addSession(this);
         }
      }

      if ((!internal) && (transactionThreadLocal.get() == null)) {
         transactionObtained = TransactionalState.addTransaction(this);
      }

      if (willEnforceBoundary() && sessionRequired && (!(sessionObtained || transactionObtained))) {
         throw new SessionEnforcementException("Session was requested outside of any boundary enforcement (@NonTransactional or @Transactional)");
      }

      return persistenceManager;
   }

   public void close () {

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