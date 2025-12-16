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
package org.smallmind.persistence.orm.aop;

import java.util.LinkedList;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.ProxyTransaction;

/**
 * Thread-local tracker for transactional boundaries and the proxy transactions they contain.
 * Supports nested boundaries and enforces allowed session sources, rollback-only semantics,
 * and coordination with non-transactional boundaries.
 */
public class TransactionalState {

  private static final ThreadLocal<LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>>> TRANSACTION_SET_STACK_LOCAL = new ThreadLocal<>();

  /**
   * @return true when any transaction is active in the current thread
   */
  public static boolean isInTransaction () {

    return isInTransaction(null);
  }

  /**
   * Determines whether a transaction for the given session source key is active.
   *
   * @param sessionSourceKey session key to check; {@code null} checks the default
   * @return true when active
   */
  public static boolean isInTransaction (String sessionSourceKey) {

    return currentTransaction(sessionSourceKey) != null;
  }

  /**
   * Returns the current transaction for the given session source key.
   *
   * @param sessionSourceKey session key to find; {@code null} finds the default
   * @return the matching transaction or {@code null} if none
   */
  public static ProxyTransaction<?> currentTransaction (String sessionSourceKey) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
      for (RollbackAwareBoundarySet<ProxyTransaction<?>> transactionSet : transactionSetStack) {
        for (ProxyTransaction<?> proxyTransaction : transactionSet) {
          if (sessionSourceKey == null) {
            if (proxyTransaction.getSession().getSessionSourceKey() == null) {

              return proxyTransaction;
            }
          } else if (sessionSourceKey.equals(proxyTransaction.getSession().getSessionSourceKey())) {

            return proxyTransaction;
          }
        }
      }
    }

    return null;
  }

  /**
   * Checks whether the given session is inside any transactional boundary.
   *
   * @param proxySession session to test
   * @return true when within a boundary that allows the session
   */
  public static boolean withinBoundary (ProxySession<?, ?> proxySession) {

    return withinBoundary(proxySession.getSessionSourceKey());
  }

  /**
   * Checks whether a session source key is inside any transactional boundary.
   *
   * @param sessionSourceKey session key to test
   * @return true when within a boundary that allows the session
   */
  public static boolean withinBoundary (String sessionSourceKey) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
      for (RollbackAwareBoundarySet<ProxyTransaction<?>> transactionSet : transactionSetStack) {
        if (transactionSet.allows(sessionSourceKey)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Finds the active boundary set that allows the given session.
   *
   * @param proxySession session to locate
   * @return the boundary set or {@code null} if none
   * @throws StolenTransactionError if a non-transactional boundary already claims the session
   */
  public static RollbackAwareBoundarySet<ProxyTransaction<?>> obtainBoundary (ProxySession<?, ?> proxySession) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) != null) {
      for (RollbackAwareBoundarySet<ProxyTransaction<?>> transactionSet : transactionSetStack) {
        if (transactionSet.allows(proxySession)) {
          if (NonTransactionalState.containsSession(proxySession)) {
            throw new StolenTransactionError("Attempt to steal the session - a non-transactional boundary is already enforced");
          }

          return transactionSet;
        }
      }
    }

    return null;
  }

  /**
   * Begins a new transactional boundary for the given annotation configuration.
   *
   * @param transactional the annotation describing the boundary
   */
  protected static void startBoundary (Transactional transactional) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) {
      TRANSACTION_SET_STACK_LOCAL.set(transactionSetStack = new LinkedList<>());
    }

    transactionSetStack.addLast(new RollbackAwareBoundarySet<>(transactional.dataSources(), transactional.implicit(), transactional.rollbackOnly()));
  }

  protected static void commitBoundary ()
    throws TransactionError {

    endBoundary(null, false);
  }

  /**
   * Ends the current transactional boundary, committing unless the supplied throwable indicates otherwise.
   *
   * @param throwable throwable captured during boundary execution; may be {@code null}
   * @throws TransactionError when commit processing fails
   */
  protected static void commitBoundary (Throwable throwable)
    throws TransactionError {

    endBoundary(throwable, false);
  }

  /**
   * Ends the current transactional boundary with an explicit rollback.
   *
   * @param throwable throwable captured during boundary execution; may be {@code null}
   * @throws TransactionError when rollback processing fails
   */
  protected static void rollbackBoundary (Throwable throwable)
    throws TransactionError {

    endBoundary(throwable, true);
  }

  /**
   * Processes the end of a transactional boundary, committing or rolling back transactions and cleaning up the stack.
   *
   * @param throwable    throwable propagated from the boundary, if any
   * @param rollbackOnly whether to force rollback
   * @throws TransactionError if boundary state is invalid or commit/rollback fails
   */
  private static void endBoundary (Throwable throwable, boolean rollbackOnly)
    throws TransactionError {

    if ((!(throwable instanceof TransactionError)) || (!((TransactionError)throwable).isTerminal())) {

      LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>> transactionSetStack;

      if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) {
        throw new TransactionBoundaryError(throwable, "No transaction boundary has been enforced");
      } else if (transactionSetStack.isEmpty()) {
        TRANSACTION_SET_STACK_LOCAL.remove();
        throw new TransactionBoundaryError(throwable, "The transaction boundary is in an inconsistent state");
      } else {
        try {

          RollbackAwareBoundarySet<ProxyTransaction<?>> transactionSet;
          IncompleteTransactionError incompleteTransactionError = null;

          for (ProxyTransaction<?> proxyTransaction : transactionSet = transactionSetStack.removeLast()) {
            try {
              if (rollbackOnly || transactionSet.isRollbackOnly() || proxyTransaction.isRollbackOnly()) {
                proxyTransaction.rollback();
              } else {
                proxyTransaction.commit();
              }
            } catch (Throwable unexpectedThrowable) {
              if ((incompleteTransactionError == null) && (!(throwable instanceof TransactionError))) {
                incompleteTransactionError = new IncompleteTransactionError(unexpectedThrowable);
              }
            }
          }

          if (incompleteTransactionError != null) {
            throw incompleteTransactionError;
          }
        } finally {
          if (transactionSetStack.isEmpty()) {
            TRANSACTION_SET_STACK_LOCAL.remove();
          }
        }
      }
    }
  }
}
