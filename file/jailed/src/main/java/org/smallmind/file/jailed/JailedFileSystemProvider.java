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
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JailedFileSystemProvider extends FileSystemProvider {

  private final JailedFileSystem jailedFileSystem;
  private final JailedPathTranslator jailedPathTranslator;
  private final String scheme;

  public JailedFileSystemProvider (String scheme, JailedPathTranslator jailedPathTranslator) {

    this.scheme = scheme;
    this.jailedPathTranslator = jailedPathTranslator;

    jailedFileSystem = new JailedFileSystem(this);
  }

  @Override
  public String getScheme () {

    return scheme;
  }

  public JailedPathTranslator getJailedPathTranslator () {

    return jailedPathTranslator;
  }

  @Override
  public FileSystem newFileSystem (URI uri, Map<String, ?> env) {

    JailedURIUtility.checkUri(scheme, uri);
    throw new FileSystemAlreadyExistsException();
  }

  @Override
  public FileSystem getFileSystem (URI uri) {

    JailedURIUtility.checkUri(scheme, uri);

    return jailedFileSystem;
  }

  @Override
  public Path getPath (URI uri) {

    return JailedURIUtility.fromUri(jailedFileSystem, uri);
  }

  @Override
  public SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().newByteChannel(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path), options, attrs);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream (Path dir, DirectoryStream.Filter<? super Path> filter)
    throws IOException {

    DirectoryStream<Path> nativeDirectoryStream = jailedPathTranslator.getNativeFileSystem().provider().newDirectoryStream(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), dir), filter);

    return new DirectoryStream<>() {

      @Override
      public Iterator<Path> iterator () {

        Iterator<Path> nativeIterator = nativeDirectoryStream.iterator();

        return new Iterator<>() {

          @Override
          public boolean hasNext () {

            return nativeIterator.hasNext();
          }

          @Override
          public Path next () {

            try {
              return jailedPathTranslator.wrapPath(jailedFileSystem, nativeIterator.next());
            } catch (IOException ioException) {
              throw new RuntimeException(ioException);
            }
          }
        };
      }

      @Override
      public void close ()
        throws IOException {

        nativeDirectoryStream.close();
      }
    };
  }

  @Override
  public void createDirectory (Path dir, FileAttribute<?>... attrs)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().createDirectory(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), dir), attrs);
  }

  @Override
  public void delete (Path path)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().delete(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path));
  }

  @Override
  public void copy (Path source, Path target, CopyOption... options)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().copy(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), source), jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), target), options);
  }

  @Override
  public void move (Path source, Path target, CopyOption... options)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().move(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), source), jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), target), options);
  }

  @Override
  public boolean isSameFile (Path path, Path path2)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().isSameFile(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path), jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path));
  }

  @Override
  public boolean isHidden (Path path)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().isHidden(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path));
  }

  @Override
  public FileStore getFileStore (Path path)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().getFileStore(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path));
  }

  @Override
  public void checkAccess (Path path, AccessMode... modes)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().checkAccess(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path), modes);
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    try {
      return jailedPathTranslator.getNativeFileSystem().provider().getFileAttributeView(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path), type, options);
    } catch (IOException ioException) {
      throw new RuntimeException(ioException);
    }
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes (Path path, Class<A> type, LinkOption... options)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().readAttributes(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path), type, options);
  }

  @Override
  public Map<String, Object> readAttributes (Path path, String attributes, LinkOption... options)
    throws IOException {

    return jailedPathTranslator.getNativeFileSystem().provider().readAttributes(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path), attributes, options);
  }

  @Override
  public void setAttribute (Path path, String attribute, Object value, LinkOption... options)
    throws IOException {

    jailedPathTranslator.getNativeFileSystem().provider().setAttribute(jailedPathTranslator.unwrapPath(jailedPathTranslator.getNativeFileSystem(), path), attribute, value, options);
  }
}
