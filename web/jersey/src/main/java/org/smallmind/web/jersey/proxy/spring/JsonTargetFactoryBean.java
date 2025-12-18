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

import java.net.URISyntaxException;
import org.smallmind.web.jersey.proxy.HttpProtocol;
import org.smallmind.web.jersey.proxy.JsonTarget;
import org.smallmind.web.jersey.proxy.JsonTargetFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring FactoryBean that constructs {@link JsonTarget} instances from simple properties.
 */
public class JsonTargetFactoryBean implements FactoryBean<JsonTarget>, InitializingBean {

  private JsonTarget target;
  private HttpProtocol protocol;
  private String host;
  private String context;
  private int port;

  /**
   * Sets the HTTP protocol to use.
   *
   * @param protocol protocol enum
   */
  public void setProtocol (HttpProtocol protocol) {

    this.protocol = protocol;
  }

  /**
   * Sets the target host name.
   *
   * @param host host name
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Sets the target port.
   *
   * @param port port number
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Sets an optional context path.
   *
   * @param context context path
   */
  public void setContext (String context) {

    this.context = context;
  }

  /**
   * Indicates this FactoryBean produces a singleton.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Returns the object type this factory creates.
   *
   * @return {@link JsonTarget} class
   */
  @Override
  public Class<?> getObjectType () {

    return JsonTarget.class;
  }

  /**
   * Builds the {@link JsonTarget} once properties are set.
   *
   * @throws URISyntaxException if the URI cannot be built
   */
  @Override
  public void afterPropertiesSet ()
    throws URISyntaxException {

    target = JsonTargetFactory.manufacture(protocol, host, port, context);
  }

  /**
   * Returns the created target instance.
   *
   * @return JsonTarget singleton
   */
  @Override
  public JsonTarget getObject () {

    return target;
  }
}
