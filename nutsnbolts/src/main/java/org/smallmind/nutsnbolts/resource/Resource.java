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

import java.io.InputStream;

/**
 * Uniform abstraction over addressable content that can be resolved to a readable
 * {@link InputStream}, decoupling callers from the underlying backing store (filesystem,
 * classpath, JAR entry, URL, etc.).
 */
public interface Resource {

  /**
   * Returns the fully qualified identifier for this resource, typically in {@code scheme:path} form.
   *
   * @return identifier string suitable for logging, caching, or equality comparisons
   */
  String getIdentifier ();

  /**
   * Returns the scheme component of the resource identifier that indicates the backing store type.
   *
   * @return scheme name, such as {@code file}, {@code classpath}, {@code jar}, or {@code url}
   */
  String getScheme ();

  /**
   * Returns the path component of the resource identifier as supplied at creation time.
   *
   * @return raw path string without the scheme prefix
   */
  String getPath ();

  /**
   * Opens the underlying content for reading.
   *
   * @return an input stream positioned at the beginning of the resource content, or {@code null}
   * if the backing store cannot locate the content
   * @throws ResourceException if the resource cannot be opened or resolved due to an error
   */
  InputStream getInputStream ()
    throws ResourceException;
}
