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
 * {@link FileSystem} wrapper that constrains access through a {@link JailedPathTranslator}.
 */
public class JailedFileSystem extends FileSystem {

  private static final String SEPARATOR = Character.toString(JailedPath.SEPARATOR);

  private final JailedFileSystemProvider jailedFileSystemProvider;
  private final JailedPath rootPath;

  /**
   * @param jailedFileSystemProvider the provider this file system belongs to
   */
  public JailedFileSystem (JailedFileSystemProvider jailedFileSystemProvider) {

    this.jailedFileSystemProvider = jailedFileSystemProvider;

    rootPath = new JailedPath(this, getSeparator().toCharArray(), true);
  }

  public FileSystemProvider provider () {

    return jailedFileSystemProvider;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close () {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOpen () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().isOpen();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReadOnly () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().isReadOnly();
  }

  @Override
  public String getSeparator () {

    return SEPARATOR;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<Path> getRootDirectories () {

    return List.of(rootPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterable<FileStore> getFileStores () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().getFileStores();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> supportedFileAttributeViews () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().supportedFileAttributeViews();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Path getPath (String first, String... more) {

    return new JailedPath(this, ((more == null) || (more.length == 0)) ? first : first + getSeparator() + String.join(getSeparator(), more));
  }

  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().getPathMatcher(syntaxAndPattern);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().getUserPrincipalLookupService();
  }

  @Override
  public WatchService newWatchService ()
    throws IOException {

    return jailedFileSystemProvider.getJailedPathTranslator().getNativeFileSystem().newWatchService();
  }
}
