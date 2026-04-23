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
 * Thread-local registry for non-transactional session boundary stacks, coordinating with
 * {@link TransactionalState} to prevent session-stealing and to ensure sessions are closed in
 * the correct order when boundaries unwind.
 */
public class NonTransactionalState {

  private static final ThreadLocal<LinkedList<BoundarySet<ProxySession<?, ?>>>> SESSION_SET_STACK_LOCAL = new ThreadLocal<>();

  /**
   * Returns {@code true} when any non-transactional session is active on the current thread.
   *
   * @return {@code true} if a non-transactional session exists for any source key
   */
  public static boolean isInSession () {

    return isInSession(null);
  }

  /**
   * Returns {@code true} when a non-transactional session for the given source key is active on the current thread.
   *
   * @param sessionSourceKey the source key to check; {@code null} checks the unnamed default source
   * @return {@code true} if a matching session is active
   */
  public static boolean isInSession (String sessionSourceKey) {

    return currentSession(sessionSourceKey) != null;
  }

  /**
   * Returns the current session for the given source key, delegating to the active transactional session first
   * if one exists.
   *
   * @param sessionSourceKey the source key to find; {@code null} finds the unnamed default source
   * @return the matching {@link ProxySession}, or {@code null} if no session is active for that key
   */
  public static ProxySession<?, ?> currentSession (String sessionSourceKey) {

    LinkedList<BoundarySet<ProxySession<?, ?>>> sessionSetStack;
    ProxyTransaction<?> currentTransaction;

    if ((currentTransaction = TransactionalState.currentTransaction(sessionSourceKey)) != null) {

      return currentTransaction.getSession();
    }

    if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) != null) {
      for (BoundarySet<ProxySession<?, ?>> sessionSet : sessionSetStack) {
        for (ProxySession<?, ?> proxySession : sessionSet) {
          if (sessionSourceKey == null) {
            if (proxySession.getSessionSourceKey() == null) {

              return proxySession;
            }
          } else if (sessionSourceKey.equals(proxySession.getSessionSourceKey())) {

            return proxySession;
          }
        }
      }
    }

    return null;
  }

  /**
   * Returns {@code true} if the given session appears in any non-transactional boundary currently on the stack
   * for the current thread.
   *
   * @param proxySession the session to search for
   * @return {@code true} if the session is present in any active non-transactional boundary
   */
  protected static boolean containsSession (ProxySession<?, ?> proxySession) {

    LinkedList<BoundarySet<ProxySession<?, ?>>> sessionSetStack;

    if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) != null) {
      for (BoundarySet<ProxySession<?, ?>> sessionSet : sessionSetStack) {
        if (sessionSet.contains(proxySession)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns the innermost non-transactional boundary set that permits the given session, or {@code null}
   * if no active boundary allows it.
   *
   * @param proxySession the session to locate
   * @return the matching {@link BoundarySet}, or {@code null} if none
   */
  public static BoundarySet<ProxySession<?, ?>> obtainBoundary (ProxySession<?, ?> proxySession) {

    LinkedList<BoundarySet<ProxySession<?, ?>>> sessionSetStack;

    if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) != null) {
      for (BoundarySet<ProxySession<?, ?>> sessionSet : sessionSetStack) {
        if (sessionSet.allows(proxySession)) {

          return sessionSet;
        }
      }
    }

    return null;
  }

  /**
   * Pushes a new non-transactional boundary onto the current thread's stack, initialized from the given annotation.
   *
   * @param nonTransactional the annotation whose {@code dataSources} and {@code implicit} attributes configure the boundary
   */
  protected static void startBoundary (NonTransactional nonTransactional) {

    LinkedList<BoundarySet<ProxySession<?, ?>>> sessionSetStack;

    if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) == null) {
      SESSION_SET_STACK_LOCAL.set(sessionSetStack = new LinkedList<>());
    }

    sessionSetStack.addLast(new BoundarySet<>(nonTransactional.dataSources(), nonTransactional.implicit()));
  }

  /**
   * Pops the most recent non-transactional boundary from the stack, closing all sessions it contains
   * and cleaning up thread-local state when the stack becomes empty.
   *
   * @param throwable the throwable that propagated from the boundary body, or {@code null} on normal return
   * @throws SessionError if the boundary stack is empty, if a session cannot be closed, or if ordering constraints are violated
   */
  protected static void endBoundary (Throwable throwable)
    throws SessionError {

    if ((!(throwable instanceof SessionError)) || ((SESSION_SET_STACK_LOCAL.get() != null) && (SESSION_SET_STACK_LOCAL.get().size() != ((SessionError)throwable).getClosure()))) {

      LinkedList<BoundarySet<ProxySession<?, ?>>> sessionSetStack;
      UnexpectedSessionError unexpectedSessionError = null;

      if (((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) == null) || sessionSetStack.isEmpty()) {
        throw new SessionBoundaryError(0, throwable, "No session boundary has been enforced");
      }

      try {
        for (ProxySession<?, ?> proxySession : sessionSetStack.removeLast()) {
          try {
            proxySession.close();
          } catch (Throwable unexpectedThrowable) {
            if (unexpectedSessionError == null) {
              unexpectedSessionError = new UnexpectedSessionError(sessionSetStack.size(), unexpectedThrowable);
            }
          }
        }

        if (unexpectedSessionError != null) {
          throw unexpectedSessionError;
        }
      } finally {
        if (sessionSetStack.isEmpty()) {
          SESSION_SET_STACK_LOCAL.remove();
        }
      }
    } else {
      SESSION_SET_STACK_LOCAL.remove();
    }
  }
}
