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

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Iterator;
import java.util.Set;
import org.smallmind.file.ephemeral.heap.DirectoryNode;

public class EphemeralDirectoryStream implements SecureDirectoryStream<Path> {

  private final EphemeralFileSystemProvider provider;
  private final EphemeralPath streamPath;
  private final DirectoryNode directoryNode;
  private final DirectoryStream.Filter<? super Path> filter;
  private boolean closed = false;

  public EphemeralDirectoryStream (EphemeralFileSystemProvider provider, EphemeralPath streamPath, DirectoryNode directoryNode, DirectoryStream.Filter<? super Path> filter) {

    this.provider = provider;
    this.streamPath = streamPath;
    this.directoryNode = directoryNode;
    this.filter = filter;
  }

  @Override
  public synchronized void close () {

    closed = true;
  }

  @Override
  public synchronized Iterator<Path> iterator () {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return directoryNode.iterator(streamPath, filter);
    }
  }

  @Override
  public synchronized SecureDirectoryStream<Path> newDirectoryStream (Path path, LinkOption... options)
    throws NoSuchFileException, NotDirectoryException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return provider.newDirectoryStream(path.isAbsolute() ? path : streamPath.resolve(path), null, options);
    }
  }

  @Override
  public synchronized SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return provider.newByteChannel(path.isAbsolute() ? path : streamPath.resolve(path), options, attrs);
    }
  }

  @Override
  public synchronized void deleteFile (Path path)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.delete(path.isAbsolute() ? path : streamPath.resolve(path));
    }
  }

  @Override
  public synchronized void deleteDirectory (Path path)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.delete(path.isAbsolute() ? path : streamPath.resolve(path));
    }
  }

  @Override
  public synchronized void move (Path src, SecureDirectoryStream<Path> target, Path targetpath)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.move(src.isAbsolute() ? src : streamPath.resolve(src), targetpath.isAbsolute() ? targetpath : target.iterator().next().resolve(targetpath));
    }
  }

  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Class<V> type) {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

    }
    return null;
  }

  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

    }
    return null;
  }
}
