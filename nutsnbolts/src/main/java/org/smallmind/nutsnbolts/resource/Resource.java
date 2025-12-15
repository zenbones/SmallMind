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
 * A uniform abstraction over addressable content that can be resolved to a readable {@link InputStream}.
 * Implementations typically wrap different backing stores (filesystem, classpath, jar, URL, etc.).
 */
public interface Resource {

  /**
   * Returns the unique identifier for this resource, usually of the form {@code scheme:path}.
   *
   * @return identifier string suitable for logging or comparison
   */
  String getIdentifier ();

  /**
   * Returns the scheme portion of the resource identifier (e.g. {@code file}, {@code classpath}).
   *
   * @return scheme name used to locate the resource
   */
  String getScheme ();

  /**
   * Returns the raw path portion of the resource identifier.
   *
   * @return path string supplied when the resource was created
   */
  String getPath ();

  /**
   * Opens the underlying content for reading.
   *
   * @return an input stream positioned at the start of the resource content; may be {@code null} if the content cannot be found
   * @throws ResourceException if the resource cannot be opened or resolved
   */
  InputStream getInputStream ()
    throws ResourceException;
}
