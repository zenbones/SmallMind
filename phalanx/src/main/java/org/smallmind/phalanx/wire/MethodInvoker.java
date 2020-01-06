/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class MethodInvoker {

  private static final Class[] EMPTY_SIGNATURE = new Class[0];
  private static final Class[] OBJECT_SIGNATURE = {Object.class};
  private final HashMap<Function, Methodology> methodMap;
  private final Class<?> serviceInterface;
  private final Object targetObject;

  public MethodInvoker (Object targetObject, Class<?> serviceInterface)
    throws NoSuchMethodException, ServiceDefinitionException {

    Class<?> endpointClass;
    Method toStringMethod;
    Method hashCodeMethod;
    Method equalsMethod;

    this.targetObject = targetObject;
    this.serviceInterface = serviceInterface;

    methodMap = new HashMap<>();
    for (Method method : serviceInterface.getMethods()) {
      methodMap.put(new Function(method), new Methodology(serviceInterface, method));
    }

    endpointClass = targetObject.getClass();

    toStringMethod = endpointClass.getMethod("toString", EMPTY_SIGNATURE);
    hashCodeMethod = endpointClass.getMethod("hashCode", EMPTY_SIGNATURE);
    equalsMethod = endpointClass.getMethod("equals", OBJECT_SIGNATURE);

    methodMap.put(new Function(toStringMethod), new Methodology(serviceInterface, toStringMethod));
    methodMap.put(new Function(hashCodeMethod), new Methodology(serviceInterface, hashCodeMethod));
    methodMap.put(new Function(equalsMethod), new Methodology(serviceInterface, equalsMethod, new SyntheticArgument("obj", Object.class)));
  }

  public Function match (Function partialFunction) {

    for (Function function : methodMap.keySet()) {
      if (function.getName().equals(partialFunction.getName())) {
        if (((partialFunction.getSignature() == null) || Arrays.equals(partialFunction.getSignature(), function.getSignature())) && ((partialFunction.getResultType() == null) || partialFunction.getResultType().equals(function.getResultType()))) {

          return function;
        }
      }
    }

    return null;
  }

  public Methodology getMethodology (Function function)
    throws MissingInvocationException {

    Methodology methodology;

    if ((methodology = methodMap.get(function)) == null) {
      throw new MissingInvocationException("No method(%s) available in service interface(%s)", function.getName(), serviceInterface.getName());
    }

    return methodology;
  }

  public Object remoteInvocation (WireContext[] contexts, Function function, Object... arguments)
    throws Exception {

    Methodology methodology;

    if ((methodology = methodMap.get(function)) == null) {
      throw new MissingInvocationException("No method(%s) available in service interface(%s)", function.getName(), serviceInterface.getName());
    }

    if ((contexts != null) && (contexts.length > 0)) {
      for (WireContext context : contexts) {
        if (context != null) {
          ContextFactory.pushContext(context);
        }
      }
    }

    try {
      return methodology.getMethod().invoke(targetObject, arguments);
    } catch (InvocationTargetException invocationTargetException) {
      if ((invocationTargetException.getCause() != null) && (invocationTargetException.getCause() instanceof Exception)) {
        throw (Exception)invocationTargetException.getCause();
      } else {
        throw invocationTargetException;
      }
    } finally {
      if ((contexts != null) && (contexts.length > 0)) {
        for (WireContext context : contexts) {
          if (context != null) {
            ContextFactory.popContext(context.getClass());
          }
        }
      }
    }
  }
}