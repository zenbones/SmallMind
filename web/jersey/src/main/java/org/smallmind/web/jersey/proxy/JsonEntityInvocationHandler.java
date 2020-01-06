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
package org.smallmind.web.jersey.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.Path;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.web.jersey.aop.EntityParam;
import org.smallmind.web.jersey.aop.Argument;
import org.smallmind.web.jersey.aop.Envelope;

public class JsonEntityInvocationHandler implements InvocationHandler {

  private final ConcurrentHashMap<Class<?>, XmlAdapter> xmlAdapterMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Method, JsonArgument[]> jsonArgumentMap = new ConcurrentHashMap<>();
  private final Level level;
  private final JsonTarget target;
  private final JsonHeaderInjector[] headerInjectors;
  private final String serviceName;
  private final String basePath;
  private final int serviceVersion;

  public JsonEntityInvocationHandler (JsonTarget target, String versionPrefix, int serviceVersion, String serviceName, Level level, JsonHeaderInjector... headerInjectors) {

    this.target = target;
    this.serviceVersion = serviceVersion;
    this.serviceName = serviceName;
    this.level = level;
    this.headerInjectors = headerInjectors;

    basePath = "/" + versionPrefix + serviceVersion + '/' + serviceName;
  }

  @Override
  public Object invoke (Object proxy, Method method, Object[] args) throws Throwable {

    JsonTarget rectifiedTarget;
    Path pathAnnotation = method.getAnnotation(Path.class);
    JsonArgument[] jsonArguments;
    Argument[] arguments;

    if ((jsonArguments = jsonArgumentMap.get(method)) == null) {
      synchronized (jsonArgumentMap) {
        if ((jsonArguments = jsonArgumentMap.get(method)) == null) {
          jsonArgumentMap.put(method, jsonArguments = constructJsonArguments(method));
        }
      }
    }

    arguments = new Argument[args.length];
    for (int index = 0; index < args.length; index++) {
      arguments[index] = new Argument(jsonArguments[index].getName(), (jsonArguments[index].getXmlAdapter() != null) ? jsonArguments[index].getXmlAdapter().marshal(args[index]) : args[index]);
    }

    rectifiedTarget = target.path(basePath + ((pathAnnotation != null) ? pathAnnotation.value() : '/' + method.getName()));
    if (headerInjectors != null) {
      for (JsonHeaderInjector headerInjector : headerInjectors) {

        JsonHeader header;

        if ((header = headerInjector.injectOnInvoke(proxy, method, args)) != null) {
          rectifiedTarget.header(header.getKey(), header.getValue());
        }
      }
    }

    return rectifiedTarget.debug(level).post(new JsonHttpEntity(new Envelope(arguments)), method.getReturnType());
  }

  private JsonArgument[] constructJsonArguments (Method method)
    throws ResourceDefinitionException, IllegalAccessException, InstantiationException {

    JsonArgument[] jsonArguments = new JsonArgument[method.getParameterTypes().length];
    int index = 0;

    for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {

      String name = null;
      XmlAdapter xmlAdapter = null;

      for (Annotation annotation : parameterAnnotations) {
        if (annotation.annotationType().equals(EntityParam.class)) {
          name = ((EntityParam)annotation).value();
        }
        if (annotation.annotationType().equals(XmlJavaTypeAdapter.class)) {
          if ((xmlAdapter = xmlAdapterMap.get(((XmlJavaTypeAdapter)annotation).value())) == null) {
            synchronized (xmlAdapterMap) {
              if ((xmlAdapter = xmlAdapterMap.get(((XmlJavaTypeAdapter)annotation).value())) == null) {
                xmlAdapterMap.put(((XmlJavaTypeAdapter)annotation).value(), xmlAdapter = ((XmlJavaTypeAdapter)annotation).value().newInstance());
              }
            }
          }
        }
      }

      if (name == null) {
        throw new ResourceDefinitionException("The method(%s) of resource interface(%s) version(%d) requires @EntityParameter annotations", method.getName(), serviceName, serviceVersion);
      }

      jsonArguments[index++] = new JsonArgument(name, xmlAdapter);
    }

    return jsonArguments;
  }
}
