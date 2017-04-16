/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.nutsnbolts.context.Context;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class WireInvocationHandler implements InvocationHandler {

  private static final Class[] EMPTY_SIGNATURE = new Class[0];
  private static final Class[] OBJECT_SIGNATURE = {Object.class};
  private static final String[] NO_NAMES = new String[0];
  private static final String[] SINGLE_OBJECT_NAME = new String[] {"obj"};
  private static OneWayConversation ONE_WAY_CONVERSATION = new OneWayConversation();
  private final RequestTransport transport;
  private final HashMap<Method, String[]> methodMap = new HashMap<>();
  private final ParameterExtractor<String> serviceGroupExtractor;
  private final ParameterExtractor<String> instanceIdExtractor;
  private final ParameterExtractor<Integer> timeoutExtractor;
  private final Class serviceInterface;
  private final String serviceName;
  private final int version;

  public WireInvocationHandler (RequestTransport transport, int version, String serviceName, Class<?> serviceInterface, ParameterExtractor<String> serviceGroupExtractor, ParameterExtractor<String> instanceIdExtractor, ParameterExtractor<Integer> timeoutExtractor)
    throws Exception {

    this.transport = transport;
    this.version = version;
    this.serviceName = serviceName;
    this.serviceInterface = serviceInterface;
    this.serviceGroupExtractor = serviceGroupExtractor;
    this.timeoutExtractor = timeoutExtractor;
    this.instanceIdExtractor = instanceIdExtractor;

    if (serviceGroupExtractor == null) {
      throw new ServiceDefinitionException("The service interface(%s) has no service group extractor %s is defined", serviceInterface.getName(), ParameterExtractor.class.getSimpleName());
    }

    for (Method method : serviceInterface.getMethods()) {

      String[] argumentNames = new String[method.getParameterTypes().length];
      int index = 0;

      for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
        for (Annotation annotation : parameterAnnotations) {
          if (annotation.annotationType().equals(Argument.class)) {
            argumentNames[index++] = ((Argument)annotation).value();
            break;
          }
        }
      }

      if (index != argumentNames.length) {
        throw new ServiceDefinitionException("The method(%s) of service interface(%s) requires @Argument annotations", method.getName(), serviceInterface.getName());
      }

      methodMap.put(method, argumentNames);
    }

    try {
      serviceInterface.getMethod("toString", EMPTY_SIGNATURE);
    } catch (NoSuchMethodException noSuchMethodException) {
      methodMap.put(Object.class.getMethod("toString", EMPTY_SIGNATURE), NO_NAMES);
    }
    try {
      serviceInterface.getMethod("hashCode", EMPTY_SIGNATURE);
    } catch (NoSuchMethodException noSuchMethodException) {
      methodMap.put(Object.class.getMethod("hashCode", EMPTY_SIGNATURE), NO_NAMES);
    }
    try {
      serviceInterface.getMethod("equals", OBJECT_SIGNATURE);
    } catch (NoSuchMethodException noSuchMethodException) {
      methodMap.put(Object.class.getMethod("equals", OBJECT_SIGNATURE), SINGLE_OBJECT_NAME);
    }
  }

  public Object invoke (Object proxy, final Method method, final Object[] args)
    throws Throwable {

    HashMap<String, Object> argumentMap = null;
    Context[] filteredContexts;
    WireContext[] wireContexts = null;
    Voice voice;
    String[] argumentNames;

    if ((argumentNames = methodMap.get(method)) == null) {
      throw new MissingInvocationException("No method(%s) available in the service interface(%s)", method.getName(), serviceInterface.getName());
    }
    if (argumentNames.length != ((args == null) ? 0 : args.length)) {
      throw new ServiceDefinitionException("The arguments for method(%s) in the service interface(%s) do not match those known from the service interface annotations", method.getName(), serviceInterface.getName());
    }

    if ((args != null) && (args.length > 0)) {
      argumentMap = new HashMap<>();
      for (int index = 0; index < args.length; index++) {
        if ((args[index] != null) && (!(args[index] instanceof Serializable))) {
          throw new TransportException("The argument(index=%d, name=%s, class=%s) is not Serializable", index, argumentNames[index], args[index].getClass().getName());
        }

        argumentMap.put(argumentNames[index], args[index]);
      }
    }

    if ((filteredContexts = ContextFactory.getContextsOn(method, WireContext.class)) != null) {

      int index = 0;

      wireContexts = new WireContext[filteredContexts.length];
      for (Context expectedContext : filteredContexts) {
        if (expectedContext instanceof WireContext) {
          wireContexts[index++] = (WireContext)expectedContext;
        }
      }
    }

    if (method.getAnnotation(Shout.class) != null) {
      voice = new Shouting(serviceGroupExtractor.getParameter(method, argumentMap, wireContexts));
    } else {

      Whisper whisper;

      if ((whisper = method.getAnnotation(Whisper.class)) != null) {
        if (instanceIdExtractor == null) {
          throw new ServiceDefinitionException("The method(%s) in service interface(%s) is marked as @Whisper but no instance id extractor %s is defined", method.getName(), serviceInterface.getName(), ParameterExtractor.class.getSimpleName());
        }

        Integer timeoutSeconds = null;

        if (timeoutExtractor != null) {
          timeoutSeconds = timeoutExtractor.getParameter(method, argumentMap, wireContexts);
        }
        if (timeoutSeconds == null) {
          timeoutSeconds = whisper.timeoutSeconds();
        }

        voice = new Whispering(serviceGroupExtractor.getParameter(method, argumentMap, wireContexts), instanceIdExtractor.getParameter(method, argumentMap, wireContexts), timeoutSeconds);
      } else if (method.getAnnotation(InOnly.class) != null) {
        if (!method.getReturnType().equals(void.class)) {
          throw new ServiceDefinitionException("The method(%s) in service interface(%s) is marked as @InOnly but does not return 'void'", method.getName(), serviceInterface.getName());
        }
        if (method.getExceptionTypes().length > 0) {
          throw new ServiceDefinitionException("The method(%s) in service interface(%s) is marked as @InOnly but declares an Exception list", method.getName(), serviceInterface.getName());
        }

        voice = new Talking(ONE_WAY_CONVERSATION, serviceGroupExtractor.getParameter(method, argumentMap, wireContexts));
      } else {

        InOut inOut = method.getAnnotation(InOut.class);
        Integer timeoutSeconds = null;

        if (timeoutExtractor != null) {
          timeoutSeconds = timeoutExtractor.getParameter(method, argumentMap, wireContexts);
        }
        if ((timeoutSeconds == null) && (inOut != null)) {
          timeoutSeconds = inOut.timeoutSeconds();
        }

        voice = new Talking(new TwoWayConversation(timeoutSeconds), serviceGroupExtractor.getParameter(method, argumentMap, wireContexts));
      }
    }

    return transport.transmit(voice, new Address(version, serviceName, new Function(method)), argumentMap, wireContexts);
  }
}