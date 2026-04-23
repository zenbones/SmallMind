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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.smallmind.nutsnbolts.context.ContextFactory;

/**
 * A {@link JailedPathTranslator} that derives the jail root path dynamically from a
 * {@link RootedFileSystemContext} stored in the current thread- or request-scoped context.
 *
 * <p>Rather than binding to a fixed root directory at construction time, this translator
 * consults {@link ContextFactory#getContext(Class)} on each call to
 * {@link #wrapPath(JailedFileSystem, Path)} and {@link #unwrapPath(Path)}. This allows the
 * jail boundary to be changed between calls (for example, between different user sessions)
 * without replacing the translator instance.
 *
 * <p>If no {@link RootedFileSystemContext} is present in the current context, or if the
 * context's root string is {@code null}, both translation methods throw a
 * {@link SecurityException} to prevent unauthorized access.
 *
 * @see RootedFileSystemContext
 * @see AbstractJailedPathTranslator
 */
public class ContextSensitiveRootedPathTranslator extends AbstractJailedPathTranslator {

  /**
   * The native file system against which all translated paths are resolved.
   */
  private final FileSystem nativeFileSystem;

  /**
   * Constructs a translator backed by the specified native file system.
   *
   * <p>The jail root is not fixed at construction time; it is read from the current
   * {@link RootedFileSystemContext} on every translation call.
   *
   * @param nativeFileSystem the native {@link FileSystem} that backs the jail
   */
  public ContextSensitiveRootedPathTranslator (FileSystem nativeFileSystem) {

    this.nativeFileSystem = nativeFileSystem;
  }

  /**
   * Returns the native file system supplied at construction time.
   *
   * @return the native {@link FileSystem}
   */
  @Override
  public FileSystem getNativeFileSystem () {

    return nativeFileSystem;
  }

  /**
   * Wraps a native path as a jailed path by using the root obtained from the current
   * {@link RootedFileSystemContext}.
   *
   * <p>The root path is resolved against the default file system via
   * {@link FileSystems#getDefault()}.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} for which the jailed path is created
   * @param nativePath       the native path to translate into the jail
   * @return the corresponding jailed {@link Path}
   * @throws SecurityException if no {@link RootedFileSystemContext} is present in the current
   *                           context, its root is {@code null}, or the native path escapes
   *                           the jail boundary
   */
  @Override
  public Path wrapPath (JailedFileSystem jailedFileSystem, Path nativePath) {

    RootedFileSystemContext rootedFileSystemContext;
    String root;

    if (((rootedFileSystemContext = ContextFactory.getContext(RootedFileSystemContext.class)) == null) || ((root = rootedFileSystemContext.getRoot()) == null)) {
      throw new SecurityException("No authorization for path");
    } else {

      return wrapPath(FileSystems.getDefault().getPath(root), jailedFileSystem, nativePath);
    }
  }

  /**
   * Resolves a jailed path back to its native representation using the root obtained from
   * the current {@link RootedFileSystemContext}.
   *
   * <p>The root path is resolved against the default file system via
   * {@link FileSystems#getDefault()}.
   *
   * @param jailedPath the jailed {@link Path} to translate to the native file system
   * @return the corresponding native {@link Path} on the backing file system
   * @throws SecurityException if no {@link RootedFileSystemContext} is present in the current
   *                           context or its root is {@code null}
   */
  @Override
  public Path unwrapPath (Path jailedPath) {

    RootedFileSystemContext rootedFileSystemContext;
    String root;

    if (((rootedFileSystemContext = ContextFactory.getContext(RootedFileSystemContext.class)) == null) || ((root = rootedFileSystemContext.getRoot()) == null)) {
      throw new SecurityException("No authorization for path");
    } else {

      return unwrapPath(FileSystems.getDefault().getPath(root), jailedPath);
    }
  }
}
