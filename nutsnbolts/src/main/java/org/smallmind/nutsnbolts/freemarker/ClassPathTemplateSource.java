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
 * Handle for a classpath-based FreeMarker template resource, lazily opened via {@link ClassLoader#getResourceAsStream} and used by {@link ClassPathTemplateLoader}.
 */
public class ClassPathTemplateSource {

  private final InputStream inputStream;
  private final ClassLoader classLoader;
  private final String name;

  /**
   * Constructs a source for the named classpath resource, opening its stream immediately via the supplied class loader.
   *
   * @param classLoader class loader used to locate and open the resource
   * @param name        classpath-relative resource path
   */
  public ClassPathTemplateSource (ClassLoader classLoader, String name) {

    this.classLoader = classLoader;
    this.name = name;

    inputStream = classLoader.getResourceAsStream(name);
  }

  /**
   * Returns {@code true} if the resource was found on the classpath during construction.
   *
   * @return {@code true} if the input stream is non-null
   */
  public boolean exists () {

    return inputStream != null;
  }

  /**
   * Returns the class loader used to locate this resource.
   *
   * @return the class loader
   */
  public ClassLoader getClassLoader () {

    return classLoader;
  }

  /**
   * Returns the classpath-relative resource name.
   *
   * @return the resource name as supplied to the constructor
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the input stream for reading the template content, or {@code null} if the resource was not found.
   *
   * @return the resource input stream, or {@code null}
   */
  public synchronized InputStream getInputStream () {

    return inputStream;
  }

  /**
   * Closes the underlying input stream if it was successfully opened.
   *
   * @throws IOException if closing the stream fails
   */
  public synchronized void close ()
    throws IOException {

    if (inputStream != null) {
      inputStream.close();
    }
  }

  /**
   * Returns a hash code derived from the class loader and resource name, suitable for use as a map key.
   *
   * @return combined hash of class loader and name
   */
  @Override
  public int hashCode () {

    return classLoader.hashCode() ^ name.hashCode();
  }

  /**
   * Returns {@code true} when the other object is a {@link ClassPathTemplateSource} with the same class loader and resource name.
   *
   * @param obj the object to compare with this source
   * @return {@code true} if both sources refer to the same resource
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ClassPathTemplateSource) && ((ClassPathTemplateSource)obj).getClassLoader().equals(classLoader) && ((ClassPathTemplateSource)obj).getName().equals(name);
  }
}
