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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.smallmind.nutsnbolts.context.ContextFactory;

/**
 * A {@link JailedPathTranslator} that derives the jail root path dynamically from a
 * {@link RootedFileSystemContext} stored in the current thread- or request-scoped context.
 *
 * <p>Rather than binding to a fixed root directory at construction time, this translator
 * consults {@link ContextFactory#getContext(Class)} on each call to
 * {@link #wrapPath(JailedFileSystem, Path)} and {@link #unwrapPath(Path, LinkOption...)}. This
 * allows the jail boundary to be changed between calls (for example, between different user
 * sessions) without replacing the translator instance.
 *
 * <p>The root is resolved against the native file system supplied at construction time - not
 * against the default file system - so that a jail over a non-default provider produces native
 * paths that provider can actually accept.
 *
 * <p>If no {@link RootedFileSystemContext} is present in the current context, if the context's
 * root string is {@code null}, or if that string does not denote an absolute path on the native
 * file system, both translation methods throw a {@link SecurityException} to prevent
 * unauthorized access.
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
   * Constructs a translator backed by the specified native file system, enforcing
   * {@link JailPolicy#STRICT}.
   *
   * <p>The jail root is not fixed at construction time; it is read from the current
   * {@link RootedFileSystemContext} on every translation call.
   *
   * @param nativeFileSystem the native {@link FileSystem} that backs the jail
   */
  public ContextSensitiveRootedPathTranslator (FileSystem nativeFileSystem) {

    this(nativeFileSystem, JailPolicy.STRICT);
  }

  /**
   * Constructs a translator backed by the specified native file system with an explicit
   * enforcement policy.
   *
   * @param nativeFileSystem the native {@link FileSystem} that backs the jail
   * @param jailPolicy       the {@link JailPolicy} to enforce, or {@code null} for
   *                         {@link JailPolicy#STRICT}
   */
  public ContextSensitiveRootedPathTranslator (FileSystem nativeFileSystem, JailPolicy jailPolicy) {

    super(jailPolicy);

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
   * Obtains the jail root for the current context, resolved against the native file system and
   * reduced to its absolute, normalized form.
   *
   * @return the native {@link Path} that defines the jail boundary for the current context
   * @throws SecurityException if no {@link RootedFileSystemContext} is present in the current
   *                           context, if its root is {@code null}, or if its root does not
   *                           denote an absolute path on the native file system
   */
  private Path getRootPath () {

    RootedFileSystemContext rootedFileSystemContext;
    String root;

    if (((rootedFileSystemContext = ContextFactory.getContext(RootedFileSystemContext.class)) == null) || ((root = rootedFileSystemContext.getRoot()) == null)) {
      throw new SecurityException("No authorization for path");
    } else {
      try {

        return normalizeRootPath(nativeFileSystem.getPath(root));
      } catch (IllegalArgumentException | UnsupportedOperationException exception) {
        throw new SecurityException("No authorization for path");
      }
    }
  }

  /**
   * Wraps a native path as a jailed path by using the root obtained from the current
   * {@link RootedFileSystemContext}.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} for which the jailed path is created
   * @param nativePath       the native path to translate into the jail
   * @return the corresponding jailed {@link Path}
   * @throws IOException       if an I/O error occurs while resolving real paths
   * @throws SecurityException if no usable {@link RootedFileSystemContext} is present in the
   *                           current context, or if the native path lies outside the jail
   */
  @Override
  public Path wrapPath (JailedFileSystem jailedFileSystem, Path nativePath)
    throws IOException {

    return wrapPath(getRootPath(), jailedFileSystem, nativePath);
  }

  /**
   * Resolves a jailed path back to its native representation using the root obtained from
   * the current {@link RootedFileSystemContext}.
   *
   * @param jailedPath the jailed {@link Path} to translate to the native file system
   * @param options    options indicating how symbolic links are handled by the operation that
   *                   the translated path will be used for
   * @return the corresponding native {@link Path} on the backing file system
   * @throws IOException       if an I/O error occurs while resolving real paths
   * @throws SecurityException if no usable {@link RootedFileSystemContext} is present in the
   *                           current context, or if the jailed path can not be confined to
   *                           the jail
   */
  @Override
  public Path unwrapPath (Path jailedPath, LinkOption... options)
    throws IOException {

    return unwrapPath(getRootPath(), jailedPath, options);
  }
}
