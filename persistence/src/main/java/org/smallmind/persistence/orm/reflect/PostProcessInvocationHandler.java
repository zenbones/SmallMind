/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.persistence.orm.reflect;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;

public class PostProcessInvocationHandler implements Serializable, InvocationHandler {

  private ProxyTransaction proxyTransaction;
  private TransactionEndState endState;
  private ProcessPriority priority;
  private Object proxyTarget;

  public PostProcessInvocationHandler (ProxyTransaction proxyTransaction, Object proxyTarget, TransactionEndState endState, ProcessPriority priority) {

    this.proxyTransaction = proxyTransaction;
    this.proxyTarget = proxyTarget;
    this.endState = endState;
    this.priority = priority;
  }

  public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable {

    if (!method.getReturnType().equals(void.class)) {
      throw new ProxyTransactionException("Attempt to post process a method(%s) of class(%s) with a non-void return type", method.getName(), proxyTarget.getClass().getName());
    }

    proxyTransaction.addPostProcess(new DelayedInvocationPostProcess(endState, priority, proxyTarget, method, args));

    return null;
  }
}