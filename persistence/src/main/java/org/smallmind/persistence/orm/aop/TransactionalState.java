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
 * Thread-local registry for transactional boundary stacks and the {@link ProxyTransaction} instances they contain.
 * Manages nested boundaries, enforces session source key constraints, coordinates rollback-only semantics,
 * and prevents session stealing by non-transactional boundaries.
 */
public class TransactionalState {

  private static final ThreadLocal<LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>>> TRANSACTION_SET_STACK_LOCAL = new ThreadLocal<>();

  /**
   * Returns {@code true} when any transaction is active on the current thread.
   *
   * @return {@code true} if a transaction exists for any source key
   */
  public static boolean isInTransaction () {

    return isInTransaction(null);
  }

  /**
   * Returns {@code true} when a transaction for the given session source key is active on the current thread.
   *
   * @param sessionSourceKey the source key to check; {@code null} checks the unnamed default source
   * @return {@code true} if a matching transaction is active
   */
  public static boolean isInTransaction (String sessionSourceKey) {

    return currentTransaction(sessionSourceKey) != null;
  }

  /**
   * Returns the currently active transaction for the given session source key.
   *
   * @param sessionSourceKey the source key to find; {@code null} finds the unnamed default source
   * @return the matching {@link ProxyTransaction}, or {@code null} if no transaction is active for that key
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
   * Returns {@code true} if the given session's source key falls within any active transactional boundary.
   *
   * @param proxySession the session whose source key is tested
   * @return {@code true} if the session is covered by an active boundary
   */
  public static boolean withinBoundary (ProxySession<?, ?> proxySession) {

    return withinBoundary(proxySession.getSessionSourceKey());
  }

  /**
   * Returns {@code true} if the given session source key falls within any active transactional boundary.
   *
   * @param sessionSourceKey the source key to test
   * @return {@code true} if the key is covered by an active boundary
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
   * Returns the innermost transactional boundary set that permits the given session, or {@code null}
   * if no active boundary allows it.
   *
   * @param proxySession the session to locate
   * @return the matching {@link RollbackAwareBoundarySet}, or {@code null} if none
   * @throws StolenTransactionError if a non-transactional boundary has already claimed the same session
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
   * Pushes a new transactional boundary onto the current thread's stack, initialized from the given annotation.
   *
   * @param transactional the annotation whose {@code dataSources}, {@code implicit}, and {@code rollbackOnly}
   *                      attributes configure the boundary
   */
  protected static void startBoundary (Transactional transactional) {

    LinkedList<RollbackAwareBoundarySet<ProxyTransaction<?>>> transactionSetStack;

    if ((transactionSetStack = TRANSACTION_SET_STACK_LOCAL.get()) == null) {
      TRANSACTION_SET_STACK_LOCAL.set(transactionSetStack = new LinkedList<>());
    }

    transactionSetStack.addLast(new RollbackAwareBoundarySet<>(transactional.dataSources(), transactional.implicit(), transactional.rollbackOnly()));
  }

  /**
   * Pops and commits the most recent transactional boundary with no associated throwable.
   *
   * @throws TransactionError if the boundary stack is missing, empty, or a transaction commit fails
   */
  protected static void commitBoundary ()
    throws TransactionError {

    endBoundary(null, false);
  }

  /**
   * Pops and commits the most recent transactional boundary, passing the given throwable through to
   * boundary cleanup without triggering a rollback.
   *
   * @param throwable the throwable captured during boundary execution, or {@code null}
   * @throws TransactionError if the boundary stack is missing, empty, or a transaction commit fails
   */
  protected static void commitBoundary (Throwable throwable)
    throws TransactionError {

    endBoundary(throwable, false);
  }

  /**
   * Pops the most recent transactional boundary and rolls back all transactions it contains.
   *
   * @param throwable the throwable that triggered the rollback, or {@code null}
   * @throws TransactionError if the boundary stack is missing, empty, or a transaction rollback fails
   */
  protected static void rollbackBoundary (Throwable throwable)
    throws TransactionError {

    endBoundary(throwable, true);
  }

  /**
   * Pops the most recent transactional boundary from the stack, committing or rolling back each contained
   * transaction, and removes thread-local state when the stack becomes empty.
   *
   * @param throwable    the throwable propagated from the boundary body, or {@code null} on normal return
   * @param rollbackOnly when {@code true}, forces rollback regardless of individual transaction state
   * @throws TransactionError if the boundary stack is in an invalid state or any commit/rollback fails
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
