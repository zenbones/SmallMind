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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * Strategy interface responsible for bidirectional translation between jailed paths and
 * paths on a backing native file system.
 *
 * <p>Implementations determine how the jail boundary is established (for example, a fixed
 * root directory or a root derived from a thread-bound context) and must ensure that any
 * path escaping the jail boundary is rejected.
 *
 * @see AbstractJailedPathTranslator
 * @see RootedPathTranslator
 * @see ContextSensitiveRootedPathTranslator
 */
public interface JailedPathTranslator {

  /**
   * Returns the native file system that backs the jail.
   *
   * <p>All file-system operations delegated by {@link JailedFileSystemProvider} ultimately
   * operate on paths within this file system.
   *
   * @return the native {@link FileSystem} being constrained by the jail
   */
  FileSystem getNativeFileSystem ();

  /**
   * Converts a native path from the backing file system into the jailed path representation
   * visible to clients of the jail.
   *
   * <p>Implementations must verify that {@code nativePath} falls within the jail boundary
   * and throw {@link SecurityException} (or an {@link IOException}) if it does not.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} for which the jailed path will be created
   * @param nativePath       the native path to translate into the jail
   * @return the corresponding jailed {@link Path}
   * @throws IOException       if an I/O error occurs during translation
   * @throws SecurityException if the native path lies outside the jail boundary
   */
  Path wrapPath (JailedFileSystem jailedFileSystem, Path nativePath)
    throws IOException;

  /**
   * Converts a jailed path back into the native path representation on the backing file system.
   *
   * <p>The returned path is suitable for direct use with the native {@link FileSystem}'s
   * provider and will reflect the full, absolute location on the underlying storage.
   *
   * @param jailedPath the jailed {@link Path} to translate into a native path
   * @return the corresponding native {@link Path} on the backing file system
   * @throws IOException if an I/O error occurs during translation
   */
  Path unwrapPath (Path jailedPath)
    throws IOException;
}
