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

import java.nio.file.Path;

/**
 * Abstract base class for {@link JailedPathTranslator} implementations that constrains
 * access to a subtree of the native file system defined by a root path.
 *
 * <p>This class provides reusable {@link #wrapPath(Path, JailedFileSystem, Path)} and
 * {@link #unwrapPath(Path, Path)} helpers that perform the segment-level translation
 * between the native path space and the jailed path space. Concrete subclasses supply
 * the specific root path (either statically or derived at call time from a context).
 *
 * @see RootedPathTranslator
 * @see ContextSensitiveRootedPathTranslator
 */
public abstract class AbstractJailedPathTranslator implements JailedPathTranslator {

  /**
   * Wraps a native path in a {@link JailedPath} by stripping the jail root prefix.
   *
   * <p>If {@code nativePath} is absolute it must begin with {@code rootPath}; any path
   * that would escape the jail causes a {@link SecurityException}. If it is relative, its
   * segments are preserved as a relative jailed path.
   *
   * @param rootPath         the native root path that defines the jail boundary
   * @param jailedFileSystem the {@link JailedFileSystem} for which the resulting path is created
   * @param nativePath       the native path to translate into the jailed path space
   * @return a {@link JailedPath} representing the same location relative to the jail root
   * @throws SecurityException if {@code nativePath} is absolute but does not start with
   *                           {@code rootPath}, indicating an attempted escape from the jail
   */
  public Path wrapPath (Path rootPath, JailedFileSystem jailedFileSystem, Path nativePath) {

    if (nativePath.isAbsolute()) {
      if (!nativePath.startsWith(rootPath)) {
        throw new SecurityException("No authorization for path");
      } else {

        StringBuilder pathBuilder = new StringBuilder();

        for (int index = rootPath.getNameCount(); index < nativePath.getNameCount(); index++) {
          pathBuilder.append(jailedFileSystem.getSeparator()).append(nativePath.getName(index));
        }

        return new JailedPath(jailedFileSystem, pathBuilder.toString());
      }
    } else {

      StringBuilder pathBuilder = new StringBuilder();

      for (int index = 0; index < nativePath.getNameCount(); index++) {
        pathBuilder.append('/').append(nativePath.getName(index));
      }

      return new JailedPath(jailedFileSystem, pathBuilder.toString());
    }
  }

  /**
   * Resolves a jailed path back to its absolute native path by appending the jailed
   * path's segments to the jail root.
   *
   * <p>This method rebuilds the path segment-by-segment using the native file system's
   * separator to ensure cross-platform correctness.
   *
   * @param rootPath   the native root path that defines the jail boundary
   * @param jailedPath the jailed path to translate back to the native file system
   * @return the absolute native {@link Path} that corresponds to {@code jailedPath}
   */
  public Path unwrapPath (Path rootPath, Path jailedPath) {

    StringBuilder pathBuilder = new StringBuilder();

    for (int index = 0; index < jailedPath.getNameCount(); index++) {
      if (index > 0) {
        pathBuilder.append(getNativeFileSystem().getSeparator());
      }
      pathBuilder.append(jailedPath.getName(index));
    }

    return rootPath.resolve(pathBuilder.toString());
  }
}
