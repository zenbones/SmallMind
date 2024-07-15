/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class NonTransactionalState {

  private static final ThreadLocal<LinkedList<BoundarySet<ProxySession<?, ?>>>> SESSION_SET_STACK_LOCAL = new ThreadLocal<>();

  public static boolean isInSession () {

    return isInSession(null);
  }

  public static boolean isInSession (String sessionSourceKey) {

    return currentSession(sessionSourceKey) != null;
  }

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

  protected static void startBoundary (NonTransactional nonTransactional) {

    LinkedList<BoundarySet<ProxySession<?, ?>>> sessionSetStack;

    if ((sessionSetStack = SESSION_SET_STACK_LOCAL.get()) == null) {
      SESSION_SET_STACK_LOCAL.set(sessionSetStack = new LinkedList<>());
    }

    sessionSetStack.addLast(new BoundarySet<>(nonTransactional.dataSources(), nonTransactional.implicit()));
  }

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
