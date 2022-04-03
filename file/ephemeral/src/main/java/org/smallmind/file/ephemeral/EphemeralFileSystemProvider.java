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
package org.smallmind.file.ephemeral;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

public class EphemeralFileSystemProvider extends FileSystemProvider {

  private final EphemeralFileSystem ephemeralFileSystem;
  private final String scheme;

  public EphemeralFileSystemProvider () {

    this("ephemeral");
  }

  public EphemeralFileSystemProvider (FileSystemProvider fileSystemProvider) {

    this(fileSystemProvider.getScheme());
  }

  public EphemeralFileSystemProvider (String scheme) {

    this.scheme = scheme;

    ephemeralFileSystem = new EphemeralFileSystem(this);
  }

  @Override
  public String getScheme () {

    return scheme;
  }

  @Override
  public FileSystem newFileSystem (URI uri, Map<String, ?> env) {

    EphemeralURIUtility.checkUri(scheme, uri);
    throw new FileSystemAlreadyExistsException();
  }

  @Override
  public FileSystem getFileSystem (URI uri) {

    EphemeralURIUtility.checkUri(scheme, uri);

    return ephemeralFileSystem;
  }

  @Override
  public Path getPath (URI uri) {

    return EphemeralURIUtility.fromUri(ephemeralFileSystem, uri);
  }

  @Override
  public SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {

    if (!EphemeralFileSystem.class.isAssignableFrom(path.getFileSystem().getClass())) {
      throw new ProviderMismatchException("The path(" + path + ") is not associated with the " + EphemeralFileSystem.class.getSimpleName());
    }

    return ((EphemeralFileSystem)path.getFileSystem()).getFileStore().newByteChannel(path, options, attrs);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter) {

    return null;
  }

  @Override
  public void createDirectory (Path dir, FileAttribute<?>... attrs) {

  }

  @Override
  public void delete (Path path) {

  }

  @Override
  public void copy (Path source, Path target, CopyOption... options) {

  }

  @Override
  public void move (Path source, Path target, CopyOption... options) {

  }

  @Override
  public boolean isSameFile (Path path, Path path2) {

    return false;
  }

  @Override
  public boolean isHidden (Path path) {

    return false;
  }

  @Override
  public FileStore getFileStore (Path path) {

    return null;
  }

  @Override
  public void checkAccess (Path path, AccessMode... modes) {

  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    return null;
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes (Path path, Class<A> type, LinkOption... options) {

    return null;
  }

  @Override
  public Map<String, Object> readAttributes (Path path, String attributes, LinkOption... options) {

    return null;
  }

  @Override
  public void setAttribute (Path path, String attribute, Object value, LinkOption... options) {

  }
}
