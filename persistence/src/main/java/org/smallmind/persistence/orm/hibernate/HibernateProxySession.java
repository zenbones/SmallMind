package org.smallmind.persistence.orm.hibernate;

import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.SessionEnforcementException;
import org.smallmind.persistence.orm.aop.BoundarySet;
import org.smallmind.persistence.orm.aop.NonTransactionalState;
import org.smallmind.persistence.orm.aop.RollbackAwareBoundarySet;
import org.smallmind.persistence.orm.aop.TransactionalState;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

public class HibernateProxySession extends ProxySession {

   private final ThreadLocal<Session> managerThreadLocal = new ThreadLocal<Session>();
   private final ThreadLocal<HibernateProxyTransaction> transactionThreadLocal = new ThreadLocal<HibernateProxyTransaction>();
   private final ThreadLocal<Boolean> boundaryOverrideThreadLocal = new ThreadLocal<Boolean>() {

      protected Boolean initialValue () {

         return false;
      }
   };

   private SessionFactory sessionFactory;

   public HibernateProxySession (String dataSourceKey, SessionFactory sessionFactory, boolean enforceBoundary, boolean willCascade) {

      super(dataSourceKey, enforceBoundary, willCascade);

      this.sessionFactory = sessionFactory;
   }

   public ClassMetadata getClassMetadata (Class entityClass) {

      return sessionFactory.getClassMetadata(entityClass);
   }

   public void setIgnoreBoundaryEnforcement (boolean ignoreBoundaryEnforcement) {

      boundaryOverrideThreadLocal.set(ignoreBoundaryEnforcement);
   }

   public HibernateProxyTransaction beginTransaction () {

      HibernateProxyTransaction proxyTransaction;

      if ((proxyTransaction = transactionThreadLocal.get()) == null) {

         Session session = getSession();

         if ((proxyTransaction = transactionThreadLocal.get()) == null) {
            proxyTransaction = new HibernateProxyTransaction(this, session.beginTransaction());
            transactionThreadLocal.set(proxyTransaction);
         }
      }

      return proxyTransaction;
   }

   public ProxyTransaction currentTransaction () {

      return transactionThreadLocal.get();
   }

   public void flush () {

      Session session;

      (session = getSession()).flush();
      session.clear();
   }

   public boolean isClosed () {

      Session session;

      return ((session = managerThreadLocal.get()) == null) || (!session.isOpen());
   }

   public Session getSession () {

      Session session;

      if ((session = managerThreadLocal.get()) == null) {
         session = sessionFactory.openSession();
         managerThreadLocal.set(session);

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

      return session;
   }

   public void close () {

      Session session;

      try {
         if ((session = managerThreadLocal.get()) != null) {
            session.close();
         }
      }
      finally {
         managerThreadLocal.set(null);
         transactionThreadLocal.set(null);
      }
   }
}