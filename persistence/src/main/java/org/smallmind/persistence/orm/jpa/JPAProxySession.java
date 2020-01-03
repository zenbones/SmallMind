/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
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

  public JPAProxySession (String dataSourceType, String sessionSourceKey, EntityManagerFactory entityManagerFactory, boolean boundaryEnforced, boolean cacheEnabled) {

    super(dataSourceType, sessionSourceKey, boundaryEnforced, cacheEnabled);

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

  public JPAProxyTransaction currentTransaction () {

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

    do {
      if ((entityManager = managerThreadLocal.get()) == null) {
        entityManager = entityManagerFactory.createEntityManager();
        managerThreadLocal.set(entityManager);

        RollbackAwareBoundarySet<ProxyTransaction<?>> transactionSet;
        BoundarySet<ProxySession<?, ?>> sessionSet;

        if ((transactionSet = TransactionalState.obtainBoundary(this)) != null) {
          try {
            transactionSet.add(beginTransaction());
          } catch (Throwable throwable) {
            close();
            throw new SessionEnforcementException(throwable);
          }
        } else if ((sessionSet = NonTransactionalState.obtainBoundary(this)) != null) {
          sessionSet.add(this);
        } else if (isBoundaryEnforced()) {
          close();
          throw new SessionEnforcementException("Session was requested outside of any boundary enforcement (@NonTransactional or @Transactional)");
        }
      } else if (!entityManager.isOpen()) {
        entityManager = null;

        managerThreadLocal.set(null);
        transactionThreadLocal.set(null);
      }
    } while (entityManager == null);

    return entityManager;
  }

  public void close () {

    EntityManager entityManager;

    try {
      if ((entityManager = managerThreadLocal.get()) != null) {
        entityManager.close();
      }
    } finally {
      managerThreadLocal.set(null);
      transactionThreadLocal.set(null);
    }
  }
}