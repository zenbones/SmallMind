package org.smallmind.cloud.transport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.nutsnbolts.context.Context;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class MethodInvoker {

   private static final Class[] EMPTY_SIGNATURE = new Class[0];
   private static final Class[] OBJECT_SIGNATURE = {Object.class};

   private Object targetObject;
   private HashMap<FauxMethod, Method> methodMap;

   public MethodInvoker (Object targetObject, Class[] proxyInterfaces)
      throws NoSuchMethodException {

      Class endpointClass;
      Method toStringMethod;
      Method hashCodeMethod;
      Method equalsMethod;

      this.targetObject = targetObject;

      methodMap = new HashMap<FauxMethod, Method>();
      for (Class proxyInterface : proxyInterfaces) {
         for (Method method : proxyInterface.getMethods()) {
            methodMap.put(new FauxMethod(method), method);
         }
      }

      endpointClass = targetObject.getClass();

      toStringMethod = endpointClass.getMethod("toString", EMPTY_SIGNATURE);
      hashCodeMethod = endpointClass.getMethod("hashCode", EMPTY_SIGNATURE);
      equalsMethod = endpointClass.getMethod("equals", OBJECT_SIGNATURE);

      methodMap.put(new FauxMethod(toStringMethod), toStringMethod);
      methodMap.put(new FauxMethod(hashCodeMethod), hashCodeMethod);
      methodMap.put(new FauxMethod(equalsMethod), equalsMethod);
   }

   public Object remoteInvocation (InvocationSignal invocationSignal)
      throws Exception {

      Method serviceMethod;

      if ((serviceMethod = methodMap.get(invocationSignal.getFauxMethod())) == null) {
         throw new MissingInvocationException();
      }

      if (invocationSignal.containsContexts()) {
         for (Context context : invocationSignal.getContexts()) {
            ContextFactory.setContext(context);
         }
      }

      try {
         return serviceMethod.invoke(targetObject, invocationSignal.getArgs());
      }
      catch (InvocationTargetException invocationTargetException) {
         if ((invocationTargetException.getCause() != null) && (invocationTargetException.getCause() instanceof Exception)) {
            throw (Exception)invocationTargetException.getCause();
         }
         else {
            throw invocationTargetException;
         }
      }
      finally {
         if (invocationSignal.containsContexts()) {
            for (Context context : invocationSignal.getContexts()) {
               ContextFactory.removeContext(context);
            }
         }
      }
   }
}
