package org.smallmind.persistence.orm.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.SessionEnforcementException;
import org.smallmind.persistence.orm.aop.BoundarySet;
import org.smallmind.persistence.orm.aop.NonTransactionalState;
import org.smallmind.persistence.orm.aop.RollbackAwareBoundarySet;
import org.smallmind.persistence.orm.aop.TransactionalState;

public class JPAProxySession extends ProxySession {

   private final ThreadLocal<EntityManager> managerThreadLocal = new ThreadLocal<EntityManager>();
   private final ThreadLocal<JPAProxyTransaction> transactionThreadLocal = new ThreadLocal<JPAProxyTransaction>();
   private final ThreadLocal<Boolean> boundaryOverrideThreadLocal = new ThreadLocal<Boolean>() {

      protected Boolean initialValue () {

         return false;
      }
   };

   private EntityManagerFactory entityManagerFactory;

   public JPAProxySession (String dataSourceKey, EntityManagerFactory entityManagerFactory, boolean enforceBoundary, boolean willCascade) {

      super(dataSourceKey, enforceBoundary, willCascade);

      this.entityManagerFactory = entityManagerFactory;
   }

   public void setIgnoreBoundaryEnforcement (boolean ignoreBoundaryEnforcement) {

      boundaryOverrideThreadLocal.set(ignoreBoundaryEnforcement);
   }

   public JPAProxyTransaction beginTransaction () {

      JPAProxyTransaction proxyTransaction;

      if ((proxyTransaction = transactionThreadLocal.get()) == null) {

         EntityManager entityManager = getEntityManager();

         if ((proxyTransaction = transactionThreadLocal.get()) == null) {
            proxyTransaction = new JPAProxyTransaction(this, entityManager.getTransaction());
            transactionThreadLocal.set(proxyTransaction);
         }
      }

      return proxyTransaction;
   }

   public ProxyTransaction currentTransaction () {

      return transactionThreadLocal.get();
   }

   public void flush () {

      EntityManager entityManager;

      (entityManager = getEntityManager()).flush();
      entityManager.clear();
   }

   public boolean isClosed () {

      EntityManager entityManager;

      return ((entityManager = managerThreadLocal.get()) == null) || (!entityManager.isOpen());
   }

   public Object getNativeSession () {

      return getEntityManager();
   }

   public EntityManager getEntityManager () {

      EntityManager entityManager;

      if ((entityManager = managerThreadLocal.get()) == null) {
         entityManager = entityManagerFactory.createEntityManager();
         managerThreadLocal.set(entityManager);

         RollbackAwareBoundarySet<ProxyTransaction> transactionSet;
         BoundarySet<ProxySession> sessionSet;

         if ((transactionSet = TransactionalState.obtainBoundary(this)) != null) {
            transactionSet.add(beginTransaction());
         }
         else if ((sessionSet = NonTransactionalState.obtainBoundary(this)) != null) {
            sessionSet.add(this);
         }
         else if ((!boundaryOverrideThreadLocal.get()) && willEnforceBoundary()) {
            close();
            throw new SessionEnforcementException("Session was requested outside of any boundary enforcement (@NonTransactional or @Transactional)");
         }
      }

      return entityManager;
   }

   public void close () {

      EntityManager entityManager;

      try {
         if ((entityManager = managerThreadLocal.get()) != null) {
            entityManager.close();
         }
      }
      finally {
         managerThreadLocal.set(null);
         transactionThreadLocal.set(null);
      }
   }
}