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
 * Spring {@link FactoryBean} that parses a resource identifier into a {@link Resource}.
 * Supports singleton semantics and lazy parsing during {@link #afterPropertiesSet()}.
 */
public class ResourceFactory implements FactoryBean<Resource>, InitializingBean {

  private static final ResourceParser RESOURCE_PARSER = new ResourceParser(new ResourceTypeResourceGenerator());

  private Resource resource;
  private String name;

  /**
   * Sets the resource identifier to be parsed (e.g. {@code file:/tmp/data.txt}).
   *
   * @param name resource identifier string
   */
  public void setName (String name) {

    this.name = name;
  }

  /**
   * A single instance is created and cached.
   *
   * @return {@code true}
   */
  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Declares the produced object type for Spring.
   *
   * @return {@link Resource}.class
   */
  @Override
  public Class<?> getObjectType () {

    return Resource.class;
  }

  /**
   * Returns the parsed resource instance.
   *
   * @return resolved {@link Resource}, or {@code null} if no name was provided
   * @throws Exception if parsing failed during initialization
   */
  @Override
  public Resource getObject ()
    throws Exception {

    return resource;
  }

  /**
   * Parses the configured {@code name} into a {@link Resource} instance.
   *
   * @throws Exception if parsing fails
   */
  @Override
  public void afterPropertiesSet ()
    throws Exception {

    resource = ((name == null) || name.isEmpty()) ? null : RESOURCE_PARSER.parseResource(name);
  }
}
