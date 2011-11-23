/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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

import java.lang.reflect.Proxy;
import org.smallmind.persistence.orm.ProcessPriority;
import org.smallmind.persistence.orm.ProxyTransaction;
import org.smallmind.persistence.orm.ProxyTransactionException;
import org.smallmind.persistence.orm.TransactionEndState;
import org.smallmind.persistence.orm.aop.TransactionalState;

public class PostProcessProxyFactory {

  public static Proxy generatePostProcessProxy (String dataSource, Class proxyInterface, Object target, TransactionEndState endState, ProcessPriority priority) {

    ProxyTransaction proxyTransaction;

    if ((proxyTransaction = TransactionalState.currentTransaction(dataSource)) == null) {
      throw new ProxyTransactionException("No current transaction for the requested data source(%s)", dataSource);
    }

    return generatePostProcessProxy(proxyTransaction, proxyInterface, target, endState, priority);
  }

  public static Proxy generatePostProcessProxy (ProxyTransaction proxyTransaction, Class proxyInterface, Object target, TransactionEndState endState, ProcessPriority priority) {

    return (Proxy)Proxy.newProxyInstance(proxyInterface.getClassLoader(), new Class[] {proxyInterface}, new PostProcessInvocationHandler(proxyTransaction, target, endState, priority));
  }
}

