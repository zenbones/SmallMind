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

