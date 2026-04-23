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
package org.smallmind.nutsnbolts.resource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring {@link FactoryBean} that parses a resource identifier string into a singleton
 * {@link Resource} instance during bean initialization.
 */
public class ResourceFactory implements FactoryBean<Resource>, InitializingBean {

  private static final ResourceParser RESOURCE_PARSER = new ResourceParser(new ResourceTypeResourceGenerator());

  private Resource resource;
  private String name;

  /**
   * Sets the resource identifier to be parsed when the bean is initialized.
   *
   * @param name resource identifier string, for example {@code file:/tmp/data.txt} or {@code classpath:/config.xml}
   */
  public void setName (String name) {

    this.name = name;
  }

  /**
   * Declares that this factory always returns the same instance.
   *
   * @return {@code true}, indicating singleton scope
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Reports the type of object this factory produces to the Spring container.
   *
   * @return {@code Resource.class}
   */
  @Override
  public Class<?> getObjectType () {

    return Resource.class;
  }

  /**
   * Returns the {@link Resource} instance resolved during {@link #afterPropertiesSet()}.
   *
   * @return the resolved {@link Resource}, or {@code null} if no identifier was configured
   * @throws Exception if the resource could not be resolved during initialization
   */
  @Override
  public Resource getObject ()
    throws Exception {

    return resource;
  }

  /**
   * Parses the configured identifier into a concrete {@link Resource} instance.
   * If the identifier is null or empty, the resolved resource is set to {@code null}.
   *
   * @throws Exception if parsing the identifier fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    resource = ((name == null) || name.isEmpty()) ? null : RESOURCE_PARSER.parseResource(name);
  }
}
