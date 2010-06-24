package org.smallmind.persistence.orm.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.SessionEnforcementException;
import org.smallmind.persistence.orm.aop.NonTransactionalState;
import org.smallmind.persistence.orm.aop.TransactionalState;

public class HibernateProxySession extends ProxySession {

   private final ThreadLocal<Session> managerThreadLocal;
   private final ThreadLocal<HibernateProxyTransaction> transactionThreadLocal;
   private SessionFactory sessionFactory;

   public HibernateProxySession (SessionFactory sessionFactory) {

      this(null, sessionFactory, false);
   }

   public HibernateProxySession (SessionFactory sessionFactory, boolean enforceBoundary) {

      this(null, sessionFactory, enforceBoundary);
   }

   public HibernateProxySession (String dataSourceKey, SessionFactory sessionFactory) {

      this(dataSourceKey, sessionFactory, false);
   }

   public HibernateProxySession (String dataSourceKey, SessionFactory sessionFactory, boolean enforceBoundary) {

      super(dataSourceKey, enforceBoundary);

      this.sessionFactory = sessionFactory;

      managerThreadLocal = new ThreadLocal<Session>();
      transactionThreadLocal = new ThreadLocal<HibernateProxyTransaction>();
   }

   public ClassMetadata getClassMetadata (Class entityClass) {

      return sessionFactory.getClassMetadata(entityClass);
   }

   public HibernateProxyTransaction beginTransaction () {

      HibernateProxyTransaction proxyTransaction;

      if ((proxyTransaction = transactionThreadLocal.get()) == null) {
         proxyTransaction = new HibernateProxyTransaction(this, getSession(true).beginTransaction());
         transactionThreadLocal.set(proxyTransaction);
      }

      return proxyTransaction;
   }

   public ProxyTransaction currentTransaction () {

      return transactionThreadLocal.get();
   }

   public void flush () {

      getSession().flush();
   }

   public boolean isClosed () {

      Session session;

      return ((session = managerThreadLocal.get()) == null) || (!session.isOpen());
   }

   public Session getSession () {

      return getSession(false);
   }

   private Session getSession (boolean internal) {

      Session session;
      boolean sessionRequired = false;
      boolean sessionObtained = false;
      boolean transactionObtained = false;

      if ((session = managerThreadLocal.get()) == null) {
         session = sessionFactory.openSession();
         managerThreadLocal.set(session);

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