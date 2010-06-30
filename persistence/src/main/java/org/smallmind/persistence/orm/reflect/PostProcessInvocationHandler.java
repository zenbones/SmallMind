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