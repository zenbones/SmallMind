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
 * Abstraction for accessing classes and resources from a classpath or external source.
 * Provides metadata such as code sources and modification times while exposing stream-based
 * access to resources.
 */
public interface ClassGate {

  long STATIC_CLASS = 0;

  /**
   * Returns the security {@link CodeSource} associated with this gate.
   *
   * @return the code source backing the gate
   * @throws Exception if the code source cannot be determined
   */
  CodeSource getCodeSource ()
    throws Exception;

  /**
   * Creates a {@link ClassStreamTicket} for the given class name, allowing controlled
   * access to the byte stream and modification time.
   *
   * @param name the fully qualified class name
   * @return a ticket describing the class stream and metadata
   * @throws Exception if the class cannot be located or read
   */
  ClassStreamTicket getTicket (String name)
    throws Exception;

  /**
   * Resolves a resource as a URL from this gate.
   *
   * @param path the resource path
   * @return a URL to the resource, or {@code null} if not found
   * @throws IOException if the resource cannot be resolved
   */
  URL getResource (String path)
    throws IOException;

  /**
   * Opens a resource as an input stream.
   *
   * @param path the resource path
   * @return an open input stream, or {@code null} if the resource does not exist
   * @throws IOException if the resource cannot be read
   */
  InputStream getResourceAsStream (String path)
    throws IOException;

  /**
   * Returns the last modification time for the supplied resource path.
   *
   * @param path the resource path
   * @return a modification timestamp, or {@link #STATIC_CLASS} when static
   * @throws Exception if the timestamp cannot be determined
   */
  long getLastModDate (String path)
    throws Exception;
}
