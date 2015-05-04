/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.jdo;

import javax.jdo.Transaction;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.TransactionPostProcessException;

public class JDOProxyTransaction extends ProxyTransaction<JDOProxySession> {

  private Transaction transaction;
  private boolean rolledBack = false;

  public JDOProxyTransaction (JDOProxySession proxySession, Transaction transaction) {

    super(proxySession);

    this.transaction = transaction;
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
    } else if (getSession().getNativeSession().isClosed()) {
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

      if (getSession().getNativeSession().isClosed()) {
        throw new ProxyTransactionException("The current Transaction can't rollback because the Session is no longer open");
      } else {
        try {
          transaction.rollback();
        } catch (Throwable throwable) {
          thrownDuringRollback = (thrownDuringRollback == null) ? throwable : (throwable.getCause() != throwable) ? throwable : throwable.initCause(thrownDuringRollback);
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
