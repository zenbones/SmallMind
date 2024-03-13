/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import jakarta.persistence.EntityTransaction;
import org.smallmind.nutsnbolts.lang.ThrowableUtility;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcessException;

public class JPAProxyTransaction extends ProxyTransaction<JPAProxySession> {

  private final EntityTransaction transaction;
  private boolean rolledBack = false;

  public JPAProxyTransaction (JPAProxySession proxySession, EntityTransaction transaction) {

    super(proxySession);

    if (!(this.transaction = transaction).isActive()) {
      this.transaction.begin();
    }
  }

  public boolean isCompleted () {

    return !transaction.isActive();
  }

  public void flush () {

    getSession().flush();
  }

  public void commit () {

    if (isRollbackOnly()) {
      rollback(new ProxyTransactionException("Transaction has been set to allow rollback only"));
    } else if (!getSession().getNativeSession().isOpen()) {
      throw new ProxyTransactionException("The current Transaction can't commit because the Session is no longer open");
    } else {
      try {
        getSession().flush();
        transaction.commit();
      } catch (Throwable throwable) {
        rollback(throwable);
      } finally {
        getSession().close();
      }

      if (!rolledBack) {
        try {
          applyPostProcesses(TransactionEndState.COMMIT);
        } catch (TransactionPostProcessException transactionPostProcessException) {
          throw new ProxyTransactionException(transactionPostProcessException);
        }
      }
    }
  }

  public void rollback () {

    rollback(null);
  }

  private void rollback (Throwable thrownDuringCommit) {

    Throwable thrownDuringRollback = thrownDuringCommit;

    if (!rolledBack) {
      rolledBack = true;

      if (!getSession().getNativeSession().isOpen()) {
        throw new ProxyTransactionException("The current Transaction can't rollback because the Session is no longer open");
      } else {
        try {
          transaction.rollback();
        } catch (Throwable throwable) {
          thrownDuringRollback = (thrownDuringRollback == null) ? throwable : ThrowableUtility.attach(throwable, thrownDuringRollback);
        } finally {
          getSession().close();

          try {
            applyPostProcesses(TransactionEndState.ROLLBACK);
          } catch (TransactionPostProcessException transactionPostProcessException) {
            thrownDuringRollback = (thrownDuringRollback == null) ? new ProxyTransactionException(transactionPostProcessException) : new ProxyTransactionException(transactionPostProcessException).initCause(thrownDuringRollback);
          }
        }

        if (thrownDuringRollback != null) {
          if (thrownDuringRollback instanceof ProxyTransactionException) {
            throw (ProxyTransactionException)thrownDuringRollback;
          } else {
            throw new ProxyTransactionException(thrownDuringRollback);
          }
        }
      }
    }
  }
}
