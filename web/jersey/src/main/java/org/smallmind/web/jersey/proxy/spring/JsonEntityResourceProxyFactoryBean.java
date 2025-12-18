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
 * Spring FactoryBean that produces a JSON resource proxy for a given interface.
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
   * Sets the target endpoint to which requests will be sent.
   *
   * @param target base {@link JsonTarget}
   */
  public void setTarget (JsonTarget target) {

    this.target = target;
  }

  /**
   * Sets optional header injectors used for each invocation.
   *
   * @param headerInjectors injectors to apply
   */
  public void setHeaderInjectors (JsonHeaderInjector[] headerInjectors) {

    this.headerInjectors = headerInjectors;
  }

  /**
   * Identifies the resource interface implemented by the proxy.
   *
   * @param resourceInterface interface class
   */
  public void setResourceInterface (Class<?> resourceInterface) {

    this.resourceInterface = resourceInterface;
  }

  /**
   * Sets the logical service name portion of the URL.
   *
   * @param serviceName service name
   */
  public void setServiceName (String serviceName) {

    this.serviceName = serviceName;
  }

  /**
   * Sets the prefix used ahead of the numeric version.
   *
   * @param versionPrefix path prefix before the version number
   */
  public void setVersionPrefix (String versionPrefix) {

    this.versionPrefix = versionPrefix;
  }

  /**
   * Sets the numeric service version.
   *
   * @param serviceVersion version number
   */
  public void setServiceVersion (int serviceVersion) {

    this.serviceVersion = serviceVersion;
  }

  /**
   * Sets the debug log level to use for requests.
   *
   * @param level log level
   */
  public void setLevel (Level level) {

    this.level = level;
  }

  /**
   * Builds the proxy once all properties are configured.
   *
   * @throws Exception if proxy creation fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    proxy = JsonEntityResourceProxyFactory.generateProxy(target, versionPrefix, serviceVersion, serviceName, resourceInterface, level, headerInjectors);
  }

  /**
   * Indicates the factory always returns a singleton instance.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the proxy type.
   *
   * @return Proxy class
   */
  @Override
  public Class<?> getObjectType () {

    return Proxy.class;
  }

  /**
   * Returns the created proxy.
   *
   * @return proxy instance
   * @throws Exception if creation failed
   */
  @Override
  public Proxy getObject ()
    throws Exception {

    return proxy;
  }
}
