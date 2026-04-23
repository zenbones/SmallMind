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
package org.smallmind.file.jailed;

import java.net.URI;
import java.nio.file.Path;

/**
 * Utility class providing URI validation and conversion helpers for the jailed file
 * system provider.
 *
 * <p>The jailed file system uses a URI of the form {@code <scheme>:///} to identify the
 * single file-system instance managed by a {@link JailedFileSystemProvider}. Individual
 * paths within the jail are represented as {@code <scheme>://<path>}.
 *
 * <p>This class is not instantiable; all methods are static.
 */
public class JailedURIUtility {

  /**
   * Validates that the supplied {@link URI} is well-formed for use with the jailed file
   * system provider identified by {@code scheme}.
   *
   * <p>A valid file-system URI must satisfy all of the following conditions:
   * <ul>
   *   <li>Its scheme matches {@code scheme} (case-insensitive).</li>
   *   <li>It has no authority component.</li>
   *   <li>It has a path component equal to {@code "/"}.</li>
   *   <li>It has no query component.</li>
   *   <li>It has no fragment component.</li>
   * </ul>
   *
   * @param scheme the expected URI scheme (e.g., {@code "jailed"})
   * @param uri    the {@link URI} to validate
   * @throws IllegalArgumentException if any of the above conditions are not met
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
   * Converts a provider URI into a {@link JailedPath} within the supplied
   * {@link JailedFileSystem}.
   *
   * <p>The URI must be absolute, hierarchical, use the scheme of the given file system's
   * provider, and must not contain an authority, fragment, or query component. The URI's
   * path component is used directly as the jailed path string.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} that will own the resulting path
   * @param uri              the absolute, hierarchical {@link URI} to convert
   * @return a {@link JailedPath} whose string value equals the path component of {@code uri}
   * @throws IllegalArgumentException if the URI is relative, opaque, uses an incompatible
   *                                  scheme, or contains an authority, fragment, or query
   *                                  component
   */
  public static Path fromUri (JailedFileSystem jailedFileSystem, URI uri) {

    if (!uri.isAbsolute()) {
      throw new IllegalArgumentException("URI is not absolute");
    } else if (uri.isOpaque()) {
      throw new IllegalArgumentException("URI is not hierarchical");
    } else {

      String scheme = uri.getScheme();

      if ((scheme == null) || !scheme.equalsIgnoreCase(jailedFileSystem.provider().getScheme())) {
        throw new IllegalArgumentException("URI does not match this provider");
      } else if (uri.getAuthority() != null) {
        throw new IllegalArgumentException("URI has an authority component");
      } else if (uri.getFragment() != null) {
        throw new IllegalArgumentException("URI has a fragment component");
      } else if (uri.getQuery() != null) {
        throw new IllegalArgumentException("URI has a query component");
      }

      return new JailedPath(jailedFileSystem, uri.getPath());
    }
  }
}
