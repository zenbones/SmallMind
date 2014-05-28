/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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

public class JPAProxySession extends ProxySession<EntityManagerFactory, EntityManager> {

  private final ThreadLocal<EntityManager> managerThreadLocal = new ThreadLocal<EntityManager>();
  private final ThreadLocal<JPAProxyTransaction> transactionThreadLocal = new ThreadLocal<JPAProxyTransaction>();

  private EntityManagerFactory entityManagerFactory;

  public JPAProxySession (String database, String dataSourceKey, EntityManagerFactory entityManagerFactory, boolean boundaryEnforced, boolean cacheEnabled) {

    super(database, dataSourceKey, boundaryEnforced, cacheEnabled);

    this.entityManagerFactory = entityManagerFactory;
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

    getEntityManager().flush();
  }

  public void clear () {

    getEntityManager().clear();
  }

  public boolean isClosed () {

    EntityManager entityManager;

    return ((entityManager = managerThreadLocal.get()) == null) || (!entityManager.isOpen());
  }

  @Override
  public EntityManagerFactory getNativeSessionFactory () {

    return entityManagerFactory;
  }

  @Override
  public EntityManager getNativeSession () {

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
        try {
          transactionSet.add(beginTransaction());
        }
        catch (Throwable throwable) {
          close();
          throw new SessionEnforcementException(throwable);
        }
      }
      else if ((sessionSet = NonTransactionalState.obtainBoundary(this)) != null) {
        sessionSet.add(this);
      }
      else if (isBoundaryEnforced()) {
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