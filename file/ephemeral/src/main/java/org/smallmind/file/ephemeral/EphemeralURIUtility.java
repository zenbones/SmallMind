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
package org.smallmind.file.ephemeral;

import java.net.URI;
import java.nio.file.Path;

/**
 * Static utility methods for validating and converting URIs associated with the ephemeral
 * file system provider. This class is not intended to be instantiated.
 */
public class EphemeralURIUtility {

  /**
   * Validates that the supplied URI is a well-formed root URI for the ephemeral provider.
   * A conforming URI must:
   * <ul>
   *   <li>have the given scheme (case-insensitive)</li>
   *   <li>have no authority component</li>
   *   <li>have a path component equal to exactly {@code "/"}</li>
   *   <li>have no query component</li>
   *   <li>have no fragment component</li>
   * </ul>
   *
   * @param scheme the expected URI scheme
   * @param uri    the URI to validate
   * @throws IllegalArgumentException if any of the above constraints are violated
   */
  public static void checkUri (String scheme, URI uri) {

    if (!uri.getScheme().equalsIgnoreCase(scheme)) {
      throw new IllegalArgumentException("URI does not match this provider");
    } else if (uri.getAuthority() != null) {
      throw new IllegalArgumentException("URI has an authority component");
    } else if (uri.getPath() == null) {
      throw new IllegalArgumentException("Path component is undefined");
    } else if (!uri.getPath().equals("/")) {
      throw new IllegalArgumentException("Path component should be '/'");
    } else if (uri.getQuery() != null) {
      throw new IllegalArgumentException("URI has a query component");
    } else if (uri.getFragment() != null) {
      throw new IllegalArgumentException("URI has a fragment component");
    }
  }

  /**
   * Converts a URI into a {@link Path} within the given ephemeral file system. The URI must
   * be absolute, hierarchical, and have no authority, fragment, or query components. Its
   * scheme must match the file system's provider scheme.
   *
   * @param ephemeralFileSystem the file system that will interpret the URI
   * @param uri                 the URI to convert; must be absolute and hierarchical
   * @return the resulting {@link Path}
   * @throws IllegalArgumentException if the URI does not conform to the above requirements
   */
  public static Path fromUri (EphemeralFileSystem ephemeralFileSystem, URI uri) {

    if (!uri.isAbsolute()) {
      throw new IllegalArgumentException("URI is not absolute");
    } else if (uri.isOpaque()) {
      throw new IllegalArgumentException("URI is not hierarchical");
    } else {

      String scheme = uri.getScheme();

      if ((scheme == null) || !scheme.equalsIgnoreCase(ephemeralFileSystem.provider().getScheme())) {
        throw new IllegalArgumentException("URI does not match this provider");
      } else if (uri.getAuthority() != null) {
        throw new IllegalArgumentException("URI has an authority component");
      } else if (uri.getFragment() != null) {
        throw new IllegalArgumentException("URI has a fragment component");
      } else if (uri.getQuery() != null) {
        throw new IllegalArgumentException("URI has a query component");
      }

      return ephemeralFileSystem.getPath(uri);
    }
  }
}
