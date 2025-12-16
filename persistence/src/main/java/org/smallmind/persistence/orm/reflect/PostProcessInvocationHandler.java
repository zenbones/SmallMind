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
package org.smallmind.persistence.orm.reflect;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;

/**
 * Invocation handler that records a method call to be executed as a transaction post-process.
 */
public class PostProcessInvocationHandler implements Serializable, InvocationHandler {

  private final ProxyTransaction proxyTransaction;
  private final TransactionEndState endState;
  private final ProcessPriority priority;
  private final Object proxyTarget;

  /**
   * Creates an invocation handler that will schedule invocations on the target for post-processing.
   *
   * @param proxyTransaction the transaction to attach post-processes to
   *                         * @param proxyTarget the underlying target object to invoke
   * @param endState         transaction end state that should trigger the invocation
   * @param priority         execution priority for the invocation
   */
  public PostProcessInvocationHandler (ProxyTransaction proxyTransaction, Object proxyTarget, TransactionEndState endState, ProcessPriority priority) {

    this.proxyTransaction = proxyTransaction;
    this.proxyTarget = proxyTarget;
    this.endState = endState;
    this.priority = priority;
  }

  /**
   * Intercepts a void-returning method call and registers it as a post-process on the transaction.
   *
   * @param proxy  the proxy instance
   * @param method method being invoked
   * @param args   arguments supplied to the method
   * @return always {@code null}
   * @throws ProxyTransactionException if the method is not void-returning
   */
  public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable {

    if (!method.getReturnType().equals(void.class)) {
      throw new ProxyTransactionException("Attempt to post process a method(%s) of class(%s) with a non-void return type", method.getName(), proxyTarget.getClass().getName());
    }

    proxyTransaction.addPostProcess(new DelayedInvocationPostProcess(endState, priority, proxyTarget, method, args));

    return null;
  }
}
