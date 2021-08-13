/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.util.Iterator;
import java.util.Set;

public class JailedFileSystem extends FileSystem {

  private final JailedFileSystemProvider jailedFileSystemProvider;
  private final FileSystem nativeFileSystem;

  public JailedFileSystem (FileSystem nativeFileSystem, JailedAccessCheck jailedAccessCheck, String scheme) {

    this.nativeFileSystem = nativeFileSystem;

    jailedFileSystemProvider = new JailedFileSystemProvider(this, jailedAccessCheck, scheme);
  }

  public FileSystem getNativeFileSystem () {

    return nativeFileSystem;
  }

  public FileSystemProvider provider () {

    return jailedFileSystemProvider;
  }

  @Override
  public void close ()
    throws IOException {

    nativeFileSystem.close();
  }

  @Override
  public boolean isOpen () {

    return nativeFileSystem.isOpen();
  }

  @Override
  public boolean isReadOnly () {

    return nativeFileSystem.isReadOnly();
  }

  @Override
  public String getSeparator () {

    return nativeFileSystem.getSeparator();
  }

  @Override
  public Iterable<Path> getRootDirectories () {

    Iterator<Path> nativeIterator = nativeFileSystem.getRootDirectories().iterator();

    return () -> new Iterator<>() {

      @Override
      public boolean hasNext () {

        return nativeIterator.hasNext();
      }

      @Override
      public Path next () {

        return new JailedPath(JailedFileSystem.this, nativeIterator.next());
      }
    };
  }

  @Override
  public Iterable<FileStore> getFileStores () {

    return nativeFileSystem.getFileStores();
  }

  @Override
  public Set<String> supportedFileAttributeViews () {

    return nativeFileSystem.supportedFileAttributeViews();
  }

  @Override
  public Path getPath (String first, String... more) {

    return new JailedPath(this, nativeFileSystem.getPath(first, more));
  }

  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    return nativeFileSystem.getPathMatcher(syntaxAndPattern);
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    return nativeFileSystem.getUserPrincipalLookupService();
  }

  @Override
  public WatchService newWatchService ()
    throws IOException {

    return nativeFileSystem.newWatchService();
  }

  @Override
  public int hashCode () {

    return super.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof JailedFileSystem); // and path translators are equivalent?
  }
}
