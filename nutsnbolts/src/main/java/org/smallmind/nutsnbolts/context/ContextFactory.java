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

import java.util.HashMap;
import java.util.Map;

public class ContextFactory {

   private static final Map<Class<? extends Context>, ContextStackThreadLocal> CONTEXT_MAP = new HashMap<Class<? extends Context>, ContextStackThreadLocal>();

   public static boolean exists (Class<? extends Context> contextClass) {

      ContextStackThreadLocal threadLocal;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(contextClass)) == null) {
            return false;
         }
      }

      return !threadLocal.get().isEmpty();
   }

   public static void pushContext (Context context) {

      ContextStackThreadLocal threadLocal;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(context.getClass())) == null) {
            CONTEXT_MAP.put(context.getClass(), threadLocal = new ContextStackThreadLocal());
         }
      }

      threadLocal.get().push(context);
   }

   public static void setContext (Context context) {

      ContextStackThreadLocal threadLocal;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(context.getClass())) == null) {
            CONTEXT_MAP.put(context.getClass(), threadLocal = new ContextStackThreadLocal());
         }
      }

      threadLocal.get().set(context);
   }

   public static Context[] getExpectedContexts (Class<?> expectingClass)
      throws ContextException {

      ExpectedContexts contextAnnotation;
      Context[] expectedContexts;
      Class<? extends Context>[] contextClasses;

      if ((contextAnnotation = expectingClass.getAnnotation(ExpectedContexts.class)) != null) {
         try {
            contextClasses = contextAnnotation.value();

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

   public boolean containsContext (Class<? extends Context> contextClass) {

      ContextStackThreadLocal threadLocal;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(contextClass)) == null) {

            return false;
         }

         return (!threadLocal.get().isEmpty());
      }
   }

   public static <C extends Context> C getContext (Class<C> contextClass)
      throws ContextException {

      ContextStackThreadLocal threadLocal;
      C context;

      synchronized (CONTEXT_MAP) {
         if ((threadLocal = CONTEXT_MAP.get(contextClass)) == null) {
            throw new ContextException("Context(%s) has not been instantiated", contextClass);
         }
      }

      if ((context = (C)threadLocal.get().peek()) == null) {
         throw new ContextException("Context(%s) has not been instantiated", contextClass);
      }

      return context;
   }

   public static Context popContext (Context context) {

      return popContext(context.getClass());
   }

   public static <C extends Context> C popContext (Class<C> contextClass) {

      ContextStackThreadLocal threadLocal;

      synchronized (CONTEXT_MAP) {
         threadLocal = CONTEXT_MAP.get(contextClass);
      }

      if (threadLocal != null) {
         return (C)threadLocal.get().pop();
      }

      return null;
   }

   private static class ContextStackThreadLocal extends InheritableThreadLocal<ContextStack> {

      @Override
      protected ContextStack initialValue () {

         return new ContextStack();
      }
   }
}