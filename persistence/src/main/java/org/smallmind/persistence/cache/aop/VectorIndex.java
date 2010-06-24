package org.smallmind.persistence.cache.aop;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.persistence.Durable;

public class VectorIndex {

   private static final ConcurrentHashMap<MethodKey, Method> METHOD_MAP = new ConcurrentHashMap<MethodKey, Method>();

   protected static long getValue (JoinPoint joinPoint, String indexParameterName) {

      String[] parameterNames;

      parameterNames = ((MethodSignature)joinPoint.getSignature()).getParameterNames();
      for (int index = 0; index < parameterNames.length; index++) {
         if (parameterNames[index].equals(indexParameterName)) {
            if (!((MethodSignature)joinPoint.getSignature()).getParameterTypes()[index].equals(long.class)) {
               throw new CacheAutomationError("The parameter(%s) must be of type 'long'", indexParameterName);
            }

            return (Long)joinPoint.getArgs()[index];
         }
      }

      throw new CacheAutomationError("The parameter(%s) was not found as part of the annotated method signature", indexParameterName);
   }

   public static long getValue (final Durable durable, String fieldName) {

      Method getMethod;
      MethodKey methodKey;
      StringBuilder methodNameBuilder = new StringBuilder("get");

      methodNameBuilder.append(Character.toUpperCase(fieldName.charAt(0)));
      methodNameBuilder.append(fieldName.substring(1));

      if ((getMethod = METHOD_MAP.get(methodKey = new MethodKey(durable.getClass(), methodNameBuilder.toString()))) == null) {

         try {
            getMethod = durable.getClass().getMethod(methodNameBuilder.toString());
         }
         catch (NoSuchMethodException noSuchMethodException) {
            throw new CacheAutomationError(noSuchMethodException);
         }

         if (!(getMethod.getReturnType().equals(long.class) || getMethod.getReturnType().equals(Long.class))) {
            throw new CacheAutomationError("The getter for field(%s) must return type 'long'", fieldName);
         }

         METHOD_MAP.put(methodKey, getMethod);
      }

      try {
         return (Long)getMethod.invoke(durable);
      }
      catch (Exception exception) {
         throw new CacheAutomationError(exception);
      }
   }

   private static class MethodKey {

      private Class methodClass;
      private String methodName;

      private MethodKey (Class methodClass, String methodName) {

         this.methodClass = methodClass;
         this.methodName = methodName;
      }

      public Class getMethodClass () {

         return methodClass;
      }

      public String getMethodName () {

         return methodName;
      }

      @Override
      public int hashCode () {

         return methodClass.hashCode() ^ methodName.hashCode();
      }

      @Override
      public boolean equals (Object obj) {

         return (obj instanceof MethodKey) && methodClass.equals(((MethodKey)obj).getMethodClass()) && methodName.equals(((MethodKey)obj).getMethodName());
      }
   }
}
