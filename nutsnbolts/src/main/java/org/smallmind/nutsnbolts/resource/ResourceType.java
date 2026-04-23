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

/**
 * Enumeration of the built-in resource types, each associating a scheme name with its
 * concrete {@link Resource} implementation class.
 */
public enum ResourceType {

  CLASSPATH("classpath", ClasspathResource.class), FILE("file", FileResource.class), JAR("jar", JarResource.class), URL("url", URLResource.class);
  private final String resourceScheme;
  private final Class<? extends Resource> resourceClass;

  /**
   * Constructs a resource type constant that binds a scheme name to its implementation class.
   *
   * @param resourceScheme the scheme identifier string, such as {@code file} or {@code classpath}
   * @param resourceClass  the concrete {@link Resource} class that handles this scheme
   */
  ResourceType (String resourceScheme, Class<? extends Resource> resourceClass) {

    this.resourceScheme = resourceScheme;
    this.resourceClass = resourceClass;
  }

  /**
   * Returns the scheme identifier string for this resource type.
   *
   * @return scheme name, for example {@code file}, {@code classpath}, {@code jar}, or {@code url}
   */
  public String getResourceScheme () {

    return resourceScheme;
  }

  /**
   * Returns the concrete {@link Resource} implementation class associated with this type.
   *
   * @return the implementation class for this resource type
   */
  public Class<? extends Resource> getResourceClass () {

    return resourceClass;
  }
}
