/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.jdo;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.SessionEnforcementException;
import org.smallmind.persistence.orm.aop.BoundarySet;
import org.smallmind.persistence.orm.aop.NonTransactionalState;
import org.smallmind.persistence.orm.aop.RollbackAwareBoundarySet;
import org.smallmind.persistence.orm.aop.TransactionalState;

public class JDOProxySession extends ProxySession<PersistenceManager> {

  private final ThreadLocal<PersistenceManager> managerThreadLocal = new ThreadLocal<PersistenceManager>();
  private final ThreadLocal<JDOProxyTransaction> transactionThreadLocal = new ThreadLocal<JDOProxyTransaction>();

  private PersistenceManagerFactory persistenceManagerFactory;

  public JDOProxySession (String dataSourceKey, PersistenceManagerFactory persistenceManagerFactor, boolean boundaryEnforced, boolean cacheEnabled) {

    super(dataSourceKey, boundaryEnforced, cacheEnabled);

    this.persistenceManagerFactory = persistenceManagerFactor;
  }

  public JDOProxyTransaction beginTransaction () {

    JDOProxyTransaction proxyTransaction;
    Transaction transaction;

    if ((proxyTransaction = transactionThreadLocal.get()) == null) {

      PersistenceManager persistenceManager = getPersistenceManager();

      if ((proxyTransaction = transactionThreadLocal.get()) == null) {
        if (!(transaction = persistenceManager.currentTransaction()).isActive()) {
          transaction.begin();
        }
        proxyTransaction = new JDOProxyTransaction(this, transaction);
        transactionThreadLocal.set(proxyTransaction);
      }
    }

    return proxyTransaction;
  }

  public ProxyTransaction currentTransaction () {

    return transactionThreadLocal.get();
  }

  public void flush () {

    getPersistenceManager().flush();
  }

  public void clear () {

    getPersistenceManager().evictAll();
  }

  public boolean isClosed () {

    PersistenceManager persistenceManager;

    return ((persistenceManager = managerThreadLocal.get()) == null) || (persistenceManager.isClosed());
  }

  public PersistenceManager getNativeSession () {

    return getPersistenceManager();
  }

  public PersistenceManager getPersistenceManager () {

    PersistenceManager persistenceManager;

    if ((persistenceManager = managerThreadLocal.get()) == null) {
      persistenceManager = persistenceManagerFactory.getPersistenceManager();
      managerThreadLocal.set(persistenceManager);

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