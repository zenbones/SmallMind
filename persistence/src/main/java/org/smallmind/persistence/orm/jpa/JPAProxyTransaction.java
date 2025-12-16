/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * Transaction wrapper around JPA {@link EntityTransaction}, adding post-processing support.
 */
public class JPAProxyTransaction extends ProxyTransaction<JPAProxySession> {

  private final EntityTransaction transaction;
  private boolean rolledBack = false;

  /**
   * Creates a JPA proxy transaction bound to the given session and entity transaction, beginning it when necessary.
   *
   * @param proxySession owning session
   * @param transaction  native transaction
   */
  public JPAProxyTransaction (JPAProxySession proxySession, EntityTransaction transaction) {

    super(proxySession);

    if (!(this.transaction = transaction).isActive()) {
      this.transaction.begin();
    }
  }

  /**
   * Indicates whether the underlying transaction is no longer active.
   *
   * @return {@code true} when the transaction has completed
   */
  @Override
  public boolean isCompleted () {

    return !transaction.isActive();
  }

  /**
   * Flushes the session.
   */
  public void flush () {

    getSession().flush();
  }

  /**
   * Commits the transaction, applying post-process callbacks on success. Rolls back when marked rollback-only.
   *
   * @throws ProxyTransactionException if commit fails or the session is closed
   */
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

  /**
   * Rolls back the transaction and applies rollback post-processors.
   */
  @Override
  public void rollback () {

    rollback(null);
  }

  /**
   * Rolls back the transaction, capturing commit/rollback exceptions and applying post-process callbacks for rollback.
   *
   * @param thrownDuringCommit optional throwable encountered during commit
   * @throws ProxyTransactionException if rollback or post-processing fails
   */
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
