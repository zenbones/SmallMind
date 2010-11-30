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
package org.smallmind.quorum.transport;

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
            ContextFactory.pushContext(context);
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
               ContextFactory.popContext(context);
            }
         }
      }
   }
}
