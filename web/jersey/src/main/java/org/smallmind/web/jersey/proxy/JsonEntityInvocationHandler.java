/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.web.jersey.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.smallmind.web.jersey.aop.EntityParam;

public class JsonEntityInvocationHandler implements InvocationHandler {

  private final ConcurrentHashMap<Method, String[]> parameterNameMap = new ConcurrentHashMap<>();
  private final WebTarget target;
  private final String serviceName;
  private final String basePath;
  private final int serviceVersion;

  public JsonEntityInvocationHandler (WebTarget target, int serviceVersion, String serviceName) {

    this.target = target;
    this.serviceVersion = serviceVersion;
    this.serviceName = serviceName;

    basePath = "/v" + serviceVersion + '/' + serviceName;
  }

  @Override
  public Object invoke (Object proxy, Method method, Object[] args) throws Throwable {

    Path pathAnnotation = method.getAnnotation(Path.class);
    Argument[] arguments;
    String[] argumentNames;

    if ((argumentNames = parameterNameMap.get(method)) == null) {
      synchronized (parameterNameMap) {
        if ((argumentNames = parameterNameMap.get(method)) == null) {
          parameterNameMap.put(method, argumentNames = constructArgumentNames(method));
        }
      }
    }

    arguments = new Argument[args.length];
    for (int index = 0; index < args.length; index++) {
      arguments[index] = new Argument(argumentNames[index], args[index]);
    }

    return target.path(basePath + ((pathAnnotation != null) ? pathAnnotation.value() : '/' + method.getName())).request(MediaType.APPLICATION_JSON).post(Entity.entity(new Envelope(arguments), MediaType.APPLICATION_JSON), method.getReturnType());
  }

  private String[] constructArgumentNames (Method method)
    throws ResourceDefinitionException {

    String[] argumentNames = new String[method.getParameterTypes().length];
    int index = 0;

    for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
      for (Annotation annotation : parameterAnnotations) {
        if (annotation.annotationType().equals(EntityParam.class)) {
          argumentNames[index++] = ((EntityParam)annotation).value();
          break;
        }
      }
    }

    if (index != argumentNames.length) {
      throw new ResourceDefinitionException("The method(%s) of resource interface(%s) version(%d) requires @EntityParameter annotations", method.getName(), serviceName, serviceVersion);
    }

    return argumentNames;
  }
}
