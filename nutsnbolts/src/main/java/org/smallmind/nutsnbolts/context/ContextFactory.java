/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.nutsnbolts.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ContextFactory {

   private static final Map<Class<? extends Context>, InheritableThreadLocal<ContextStack>> CONTEXT_MAP = new HashMap<Class<? extends Context>, InheritableThreadLocal<ContextStack>>();
   private static final Class[] EMPTY_SIGNATURE = new Class[0];
   private static final Object[] NO_PARAMETERS = new Object[0];

   public static boolean exists (Class<? extends Context> contextClass) {

      InheritableThreadLocal<ContextStack> threadLocal;
      ContextStack contextStack;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(contextClass)) == null) {
            return false;
         }
      }

      return !(((contextStack = threadLocal.get()) == null) || contextStack.isEmpty());
   }

   public static void setContext (Context context) {

      InheritableThreadLocal<ContextStack> threadLocal;
      ContextStack contextStack;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(context.getClass())) == null) {
            threadLocal = new InheritableThreadLocal<ContextStack>();
            CONTEXT_MAP.put(context.getClass(), threadLocal);
         }
      }

      if ((contextStack = threadLocal.get()) == null) {
         contextStack = new ContextStack();
         threadLocal.set(contextStack);
      }

      contextStack.push(context);
   }

   public static Context[] getExpectedContexts (Class<?> expectingClass)
      throws ContextException {

      Context[] expectedContexts;
      Annotation expectedAnnotation;
      Method valueMethod;
      Class<? extends Context>[] contextClasses;

      if ((expectedAnnotation = expectingClass.getAnnotation(ExpectedContexts.class)) != null) {
         try {
            valueMethod = expectedAnnotation.annotationType().getMethod("value", EMPTY_SIGNATURE);
         }
         catch (NoSuchMethodException noSuchMethodException) {
            throw new ContextException(noSuchMethodException, "The annotation @ExpectedContexts has been altered and has no value() method");
         }

         try {
            contextClasses = (Class<? extends Context>[])valueMethod.invoke(expectedAnnotation, NO_PARAMETERS);

            expectedContexts = new Context[contextClasses.length];
            for (int count = 0; count < contextClasses.length; count++) {
               expectedContexts[count] = getContext(contextClasses[count]);
            }

            return expectedContexts;
         }
         catch (Exception exception) {
            throw new ContextException(exception);
         }
      }

      return null;
   }

   public static Context getContext (Class<? extends Context> contextClass)
      throws ContextException {

      InheritableThreadLocal<ContextStack> threadLocal;
      ContextStack contextStack;
      Context context;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(contextClass)) == null) {
            throw new ContextException("Context(%s) has not been instantiated", contextClass);
         }
      }

      if (((contextStack = threadLocal.get()) == null) || ((context = contextStack.peek()) == null)) {
         throw new ContextException("Context(%s) has not been instantiated", contextClass);
      }

      return context;
   }

   public static Context removeContext (Context context) {

      return removeContext(context.getClass());
   }

   public static Context removeContext (Class<? extends Context> contextClass) {

      InheritableThreadLocal<ContextStack> threadLocal;
      ContextStack contextStack;

      synchronized (CONTEXT_MAP) {
         threadLocal = CONTEXT_MAP.get(contextClass);
      }

      if ((threadLocal != null) && ((contextStack = threadLocal.get()) != null)) {
         return contextStack.pop();
      }

      return null;
   }
}