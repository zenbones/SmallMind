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
package org.smallmind.nutsnbolts.freemarker;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a classpath-based FreeMarker template source and its underlying stream.
 */
public class ClassPathTemplateSource {

  private final InputStream inputStream;
  private final ClassLoader classLoader;
  private final String name;

  /**
   * Creates a template source for a classpath resource.
   *
   * @param classLoader loader used to resolve the resource
   * @param name        resource path within the classpath
   */
  public ClassPathTemplateSource (ClassLoader classLoader, String name) {

    this.classLoader = classLoader;
    this.name = name;

    inputStream = classLoader.getResourceAsStream(name);
  }

  /**
   * @return {@code true} if the resource exists
   */
  public boolean exists () {

    return inputStream != null;
  }

  /**
   * @return class loader used to locate the resource
   */
  public ClassLoader getClassLoader () {

    return classLoader;
  }

  /**
   * @return resource name/path
   */
  public String getName () {

    return name;
  }

  /**
   * @return input stream for reading the template; may be {@code null} if resource absent
   */
  public synchronized InputStream getInputStream () {

    return inputStream;
  }

  /**
   * Closes the underlying input stream, if present.
   *
   * @throws IOException if closure fails
   */
  public synchronized void close ()
    throws IOException {

    if (inputStream != null) {
      inputStream.close();
    }
  }

  /**
   * Computes a hash combining class loader and resource name.
   *
   * @return hash code for map/set usage
   */
  @Override
  public int hashCode () {

    return classLoader.hashCode() ^ name.hashCode();
  }

  /**
   * Considers two sources equal when both class loader and resource name match.
   *
   * @param obj object to compare
   * @return {@code true} when equivalent
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ClassPathTemplateSource) && ((ClassPathTemplateSource)obj).getClassLoader().equals(classLoader) && ((ClassPathTemplateSource)obj).getName().equals(name);
  }
}
