/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import java.lang.reflect.Proxy;
import org.smallmind.scribe.pen.Level;

/**
 * Creates dynamic proxies that forward interface method calls as JSON-over-HTTP requests to a Jersey endpoint.
 */
public class JsonEntityResourceProxyFactory {

  /**
   * Generates a proxy using {@link Level#OFF} for debug logging.
   *
   * @param target            base target endpoint
   * @param versionPrefix     path segment placed before the version number
   * @param serviceVersion    numeric service version
   * @param serviceName       service name segment of the URL path
   * @param resourceInterface interface the proxy will implement
   * @param headerInjectors   zero or more header injectors applied per invocation
   * @return proxy implementing {@code resourceInterface}
   */
  public static Proxy generateProxy (JsonTarget target, String versionPrefix, int serviceVersion, String serviceName, Class<?> resourceInterface, JsonHeaderInjector... headerInjectors) {

    return generateProxy(target, versionPrefix, serviceVersion, serviceName, resourceInterface, Level.OFF, headerInjectors);
  }

  /**
   * Generates a proxy that wraps invocations as envelope-encoded JSON POST requests and registers it in {@link JsonEntityResourceProxyManager}.
   *
   * @param target            base target endpoint
   * @param versionPrefix     path segment placed before the version number
   * @param serviceVersion    numeric service version
   * @param serviceName       service name segment of the URL path
   * @param resourceInterface interface the proxy will implement
   * @param level             log level used for request/response tracing
   * @param headerInjectors   zero or more header injectors applied per invocation
   * @return proxy implementing {@code resourceInterface}
   */
  public static Proxy generateProxy (JsonTarget target, String versionPrefix, int serviceVersion, String serviceName, Class<?> resourceInterface, Level level, JsonHeaderInjector... headerInjectors) {

    Proxy proxy = (Proxy)Proxy.newProxyInstance(resourceInterface.getClassLoader(), new Class[] {resourceInterface}, new JsonEntityInvocationHandler(target, versionPrefix, serviceVersion, serviceName, level, headerInjectors));

    JsonEntityResourceProxyManager.register(resourceInterface, proxy);

    return proxy;
  }
}
