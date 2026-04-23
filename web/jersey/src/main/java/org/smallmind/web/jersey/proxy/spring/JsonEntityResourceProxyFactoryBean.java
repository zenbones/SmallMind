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
package org.smallmind.web.jersey.proxy.spring;

import java.lang.reflect.Proxy;
import org.smallmind.scribe.pen.Level;
import org.smallmind.web.jersey.proxy.JsonEntityResourceProxyFactory;
import org.smallmind.web.jersey.proxy.JsonHeaderInjector;
import org.smallmind.web.jersey.proxy.JsonTarget;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that creates and exposes a JSON entity resource proxy for a configured service interface.
 */
public class JsonEntityResourceProxyFactoryBean implements FactoryBean<Proxy>, InitializingBean {

  private Proxy proxy;
  private JsonTarget target;
  private JsonHeaderInjector[] headerInjectors;
  private Level level = Level.OFF;
  private Class<?> resourceInterface;
  private String versionPrefix = "v";
  private String serviceName;
  private int serviceVersion;

  /**
   * Sets the base HTTP target to which the proxy will send requests.
   *
   * @param target base {@link JsonTarget}
   */
  public void setTarget (JsonTarget target) {

    this.target = target;
  }

  /**
   * Sets header injectors that will be applied on every proxy invocation.
   *
   * @param headerInjectors array of injectors
   */
  public void setHeaderInjectors (JsonHeaderInjector[] headerInjectors) {

    this.headerInjectors = headerInjectors;
  }

  /**
   * Sets the interface that the generated proxy will implement.
   *
   * @param resourceInterface service interface class
   */
  public void setResourceInterface (Class<?> resourceInterface) {

    this.resourceInterface = resourceInterface;
  }

  /**
   * Sets the service name used in URL path construction.
   *
   * @param serviceName service name segment
   */
  public void setServiceName (String serviceName) {

    this.serviceName = serviceName;
  }

  /**
   * Sets the string prefixed to the version number in the URL path (defaults to {@code "v"}).
   *
   * @param versionPrefix version path prefix
   */
  public void setVersionPrefix (String versionPrefix) {

    this.versionPrefix = versionPrefix;
  }

  /**
   * Sets the numeric API version included in the URL path.
   *
   * @param serviceVersion version number
   */
  public void setServiceVersion (int serviceVersion) {

    this.serviceVersion = serviceVersion;
  }

  /**
   * Sets the log level used for request/response debug tracing.
   *
   * @param level desired log level
   */
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * Constructs the proxy once all required properties have been injected.
   *
   * @throws Exception if proxy generation fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    proxy = JsonEntityResourceProxyFactory.generateProxy(target, versionPrefix, serviceVersion, serviceName, resourceInterface, level, headerInjectors);
  }

  /**
   * Indicates that this factory always returns the same proxy instance.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the type of object produced by this factory.
   *
   * @return {@link Proxy} class
   */
  @Override
  public Class<?> getObjectType () {

    return Proxy.class;
  }

  /**
   * Returns the constructed proxy.
   *
   * @return proxy singleton
   * @throws Exception if the proxy was not successfully created
   */
  @Override
  public Proxy getObject ()
    throws Exception {

    return proxy;
  }
}
