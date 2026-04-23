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
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Set;

/**
 * A {@link FileSystem} implementation that presents a jailed, chroot-like view of an
 * underlying native file system.
 *
 * <p>All paths created by this file system are {@link JailedPath} instances whose segments
 * are strictly confined to the subtree defined by the {@link JailedPathTranslator} held by
 * the owning {@link JailedFileSystemProvider}. The path separator is always {@code '/'}.
 *
 * <p>Lifecycle operations ({@link #isOpen()}, {@link #isReadOnly()}) and metadata queries
 * ({@link #getFileStores()}, {@link #supportedFileAttributeViews()},
 * {@link #getUserPrincipalLookupService()}, {@link #newWatchService()}) are delegated to
 * the backing native file system obtained via the translator.
 *
 * @see JailedFileSystemProvider
 * @see JailedPathTranslator
 */
public class JailedFileSystem extends FileSystem {

  /**
   * The string representation of the jailed path separator character.
   */
  private static final String SEPARATOR = Character.toString(JailedPath.SEPARATOR);

  /**
   * The provider that created and manages this file system instance.
   */
  private final JailedFileSystemProvider jailedFileSystemProvider;

  /**
   * The root path of this jail, always {@code "/"}.
   */
  private final JailedPath rootPath;

  /**
   * Constructs a new jailed file system owned by the given provider.
   *
   * <p>A root {@link JailedPath} ({@code "/"}) is created eagerly and returned by
   * {@link #getRootDirectories()}.
   *
   * @param jailedFileSystemProvider the {@link JailedFileSystemProvider} that owns this
   *                                 file system; must not be {@code null}
   */
  public JailedFileSystem (JailedFileSystemProvider jailedFileSystemProvider) {

    this.jailedFileSystemProvider = jailedFileSystemProvider;

    rootPath = new JailedPath(this, getSeparator().toCharArray(), true);
  }

  /**
   * Returns the provider that created this file system.
   *
   * @return the {@link JailedFileSystemProvider} that owns this instance
   */
  public FileSystemProvider provider () {

    return jailedFileSystemProvider;
  }

  /**
   * Closes this file system.
   *
   * <p>This implementation is a no-op because the jailed file system does not own the
   * underlying native file system; its lifecycle is managed externally.
   */
  @Override
  public void close () {

  }

  /**
   * Delegates to the backing native file system to determine whether it is still open.
   *
   * @return {@code true} if the backing native file system is open
   */
  @Override
  public boolean isOpen () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().isOpen();
  }

  /**
   * Delegates to the backing native file system to determine read-only status.
   *
   * @return {@code true} if the backing native file system is read-only
   */
  @Override
  public boolean isReadOnly () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().isReadOnly();
  }

  /**
   * Returns the name separator for this file system, which is always {@code "/"}.
   *
   * @return the string {@code "/"}
   */
  @Override
  public String getSeparator () {

    return SEPARATOR;
  }

  /**
   * Returns an iterable containing only the single jail root path ({@code "/"}).
   *
   * @return an {@link Iterable} containing the root {@link JailedPath}
   */
  @Override
  public Iterable<Path> getRootDirectories () {

    return List.of(rootPath);
  }

  /**
   * Returns the file stores accessible through the backing native file system.
   *
   * @return the file stores of the backing native file system
   */
  @Override
  public Iterable<FileStore> getFileStores () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().getFileStores();
  }

  /**
   * Returns the set of supported file attribute view names from the backing native file system.
   *
   * @return the set of supported file attribute view name strings
   */
  @Override
  public Set<String> supportedFileAttributeViews () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().supportedFileAttributeViews();
  }

  /**
   * Constructs a {@link JailedPath} by joining {@code first} and the optional {@code more}
   * components with the jail separator.
   *
   * @param first the first path component
   * @param more  optional additional path components to append
   * @return a new {@link JailedPath} for the combined path string
   */
  @Override
  public Path getPath (String first, String... more) {

    return new JailedPath(this, ((more == null) || (more.length == 0)) ? first : first + getSeparator() + String.join(getSeparator(), more));
  }

  /**
   * Returns a {@link PathMatcher} for the given syntax-and-pattern string.
   *
   * <p>Delegates to the backing native file system, which determines the supported
   * syntaxes (typically {@code "glob"} and {@code "regex"}).
   *
   * @param syntaxAndPattern a string of the form {@code "<syntax>:<pattern>"}
   * @return a {@link PathMatcher} that matches paths against the given pattern
   * @throws IllegalArgumentException if the syntax is not recognized
   */
  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().getPathMatcher(syntaxAndPattern);
  }

  /**
   * Returns the {@link UserPrincipalLookupService} of the backing native file system.
   *
   * @return the user/group lookup service
   * @throws UnsupportedOperationException if the backing file system does not support
   *                                       user/group lookups
   */
  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().getUserPrincipalLookupService();
  }

  /**
   * Creates a new {@link WatchService} by delegating to the backing native file system.
   *
   * @return a new {@link WatchService}
   * @throws IOException                   if an I/O error occurs creating the watch service
   * @throws UnsupportedOperationException if the backing file system does not support
   *                                       watching file-tree changes
   */
  @Override
  public WatchService newWatchService ()
    throws IOException {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().newWatchService();
  }
}
