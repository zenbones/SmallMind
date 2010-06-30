package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class OffloadingInvocationHandler implements InvocationHandler {

   private Object target;

   public OffloadingInvocationHandler (Object target) {

      this.target = target;
   }

   @Override
   public Object invoke (Object proxy, Method method, Object[] args)
      throws Throwable {

      return method.invoke(target, args);
   }
}
