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

/**
 * A {@link JailedPathTranslator} that confines jailed paths to a fixed native root directory
 * supplied at construction time.
 *
 * <p>Unlike {@link ContextSensitiveRootedPathTranslator}, this implementation binds the jail
 * boundary to a single {@link Path} for the lifetime of the translator instance. It is
 * appropriate when the root directory is known statically and does not need to vary per
 * caller or per request.
 *
 * <p>The supplied root is made absolute and normalized at construction time, so that the
 * boundary against which every translation is checked is itself free of {@code .} and
 * {@code ..} elements.
 *
 * @see AbstractJailedPathTranslator
 * @see ContextSensitiveRootedPathTranslator
 */
public class RootedPathTranslator extends AbstractJailedPathTranslator {

  /**
   * The file system that owns the root path, and therefore backs the jail.
   */
  private final FileSystem nativeFileSystem;

  /**
   * The absolute, normalized native root path that defines the jail boundary.
   */
  private final Path rootPath;

  /**
   * Constructs a translator with a fixed jail root, enforcing {@link JailPolicy#STRICT}.
   *
   * @param rootPath the native {@link Path} that serves as the root of the jail;
   *                 all jailed paths are resolved relative to this directory
   * @throws IllegalArgumentException if {@code rootPath} is {@code null} or can not be made
   *                                  absolute
   */
  public RootedPathTranslator (Path rootPath) {

    this(rootPath, JailPolicy.STRICT);
  }

  /**
   * Constructs a translator with a fixed jail root and an explicit enforcement policy.
   *
   * @param rootPath   the native {@link Path} that serves as the root of the jail;
   *                   all jailed paths are resolved relative to this directory
   * @param jailPolicy the {@link JailPolicy} to enforce, or {@code null} for
   *                   {@link JailPolicy#STRICT}
   * @throws IllegalArgumentException if {@code rootPath} is {@code null} or can not be made
   *                                  absolute
   */
  public RootedPathTranslator (Path rootPath, JailPolicy jailPolicy) {

    super(jailPolicy);

    if (rootPath == null) {
      throw new IllegalArgumentException("The root path must not be null");
    }

    this.nativeFileSystem = rootPath.getFileSystem();
    this.rootPath = normalizeRootPath(rootPath);
  }

  /**
   * Returns the file system that owns the root path supplied at construction time.
   *
   * @return the {@link FileSystem} of the configured root path
   */
  @Override
  public FileSystem getNativeFileSystem () {

    return nativeFileSystem;
  }

  /**
   * Returns the absolute, normalized jail root.
   *
   * @return the native {@link Path} that defines the jail boundary
   */
  public Path getRootPath () {

    return rootPath;
  }

  /**
   * Translates a native path into the jailed path space using the fixed root path configured
   * at construction time.
   *
   * @param jailedFileSystem the {@link JailedFileSystem} for which the jailed path is created
   * @param nativePath       the native path to translate into the jail
   * @return the corresponding jailed {@link Path}
   * @throws IOException       if an I/O error occurs while resolving real paths
   * @throws SecurityException if {@code nativePath} is absolute and does not lie within the
   *                           configured root
   */
  @Override
  public Path wrapPath (JailedFileSystem jailedFileSystem, Path nativePath)
    throws IOException {

    return wrapPath(rootPath, jailedFileSystem, nativePath);
  }

  /**
   * Resolves a jailed path back to its absolute native path using the fixed root path
   * configured at construction time.
   *
   * @param jailedPath the jailed {@link Path} to translate back to the native file system
   * @param options    options indicating how symbolic links are handled by the operation that
   *                   the translated path will be used for
   * @return the corresponding native {@link Path} resolved against the configured root
   * @throws IOException       if an I/O error occurs while resolving real paths
   * @throws SecurityException if {@code jailedPath} can not be confined to the jail
   */
  @Override
  public Path unwrapPath (Path jailedPath, LinkOption... options)
    throws IOException {

    return unwrapPath(rootPath, jailedPath, options);
  }
}
