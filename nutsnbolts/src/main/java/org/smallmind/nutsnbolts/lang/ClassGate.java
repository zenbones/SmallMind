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
package org.smallmind.nutsnbolts.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;

/**
 * Abstraction for locating and streaming class bytes and associated resources from a classpath entry or other class source.
 */
public interface ClassGate {

  /**
   * Sentinel timestamp indicating a class has no meaningful modification date (e.g. it lives inside a JAR).
   */
  long STATIC_CLASS = 0;

  /**
   * Returns the {@link CodeSource} associated with this gate, used when defining classes with security information.
   *
   * @return the code source, or {@code null} if no code source is available
   * @throws Exception if the code source cannot be determined
   */
  CodeSource getCodeSource ()
    throws Exception;

  /**
   * Returns a {@link ClassStreamTicket} providing access to the byte stream and modification timestamp for the named class.
   *
   * @param name the fully qualified binary class name
   * @return a ticket wrapping the class byte stream and its timestamp, or {@code null} if the class is not found
   * @throws Exception if an error occurs while locating or opening the class
   */
  ClassStreamTicket getTicket (String name)
    throws Exception;

  /**
   * Resolves a classpath resource to a {@link URL}.
   *
   * @param path the path of the resource to locate
   * @return a {@link URL} pointing to the resource, or {@code null} if the resource is not found
   * @throws IOException if an I/O error occurs during resolution
   */
  URL getResource (String path)
    throws IOException;

  /**
   * Opens a classpath resource as an {@link InputStream}.
   *
   * @param path the path of the resource to open
   * @return an open stream to the resource, or {@code null} if the resource is not found
   * @throws IOException if an I/O error occurs while opening the stream
   */
  InputStream getResourceAsStream (String path)
    throws IOException;

  /**
   * Returns the last-modified timestamp in milliseconds for the resource at the given path.
   *
   * @param path the path of the resource to check
   * @return the last-modified time in milliseconds, or {@link #STATIC_CLASS} when the resource has no modification date
   * @throws Exception if the timestamp cannot be determined
   */
  long getLastModDate (String path)
    throws Exception;
}
