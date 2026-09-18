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
package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Iterator;
import java.util.Set;
import org.smallmind.file.ephemeral.heap.DirectoryNode;

/**
 * {@link SecureDirectoryStream} over the entries of an ephemeral directory.
 *
 * <p>Relative paths handed to the operations of this stream are resolved against the directory the
 * stream was opened on, which is the point of a secure directory stream: a caller can work with
 * entries by simple name without re-resolving the containing directory each time.
 */
public class EphemeralDirectoryStream implements SecureDirectoryStream<Path> {

  private final EphemeralFileStore fileStore;
  private final EphemeralFileSystemProvider provider;
  private final EphemeralPath streamPath;
  private final DirectoryNode directoryNode;
  private final DirectoryStream.Filter<? super Path> filter;
  private boolean iterated = false;
  private boolean closed = false;

  /**
   * Creates a stream over a directory.
   *
   * @param fileStore     the store the stream was opened against, notified when it closes
   * @param provider      the provider used to carry out operations on entries
   * @param streamPath    the absolute path of the directory
   * @param directoryNode the directory node being listed
   * @param filter        the filter deciding which entries to include, or {@code null} for all
   */
  public EphemeralDirectoryStream (EphemeralFileStore fileStore, EphemeralFileSystemProvider provider, EphemeralPath streamPath, DirectoryNode directoryNode, DirectoryStream.Filter<? super Path> filter) {

    this.fileStore = fileStore;
    this.provider = provider;
    this.streamPath = streamPath;
    this.directoryNode = directoryNode;
    this.filter = filter;
  }

  /**
   * Throws if this stream has been closed.
   *
   * @throws ClosedDirectoryStreamException if the stream is closed
   */
  private void ensureOpen () {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    }
  }

  /**
   * Resolves a path against this stream's directory when it is relative.
   *
   * @param path the path supplied by the caller
   * @return an absolute path
   */
  private Path against (Path path) {

    return path.isAbsolute() ? path : streamPath.resolve(path);
  }

  @Override
  public synchronized void close () {

    if (!closed) {
      closed = true;
      fileStore.unregisterOpenResource(this);
    }
  }

  /**
   * Returns an iterator over the entries of this directory.
   *
   * @return an iterator over the accepted entries
   * @throws IllegalStateException          if an iterator has already been returned
   * @throws ClosedDirectoryStreamException if the stream is closed
   */
  @Override
  public synchronized Iterator<Path> iterator () {

    ensureOpen();

    if (iterated) {
      throw new IllegalStateException("An iterator has already been obtained from this stream");
    } else {
      iterated = true;

      return directoryNode.iterator(streamPath, filter);
    }
  }

  @Override
  public synchronized SecureDirectoryStream<Path> newDirectoryStream (Path path, LinkOption... options)
    throws IOException {

    ensureOpen();

    return provider.newDirectoryStream(against(path), null, options);
  }

  @Override
  public synchronized SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    ensureOpen();

    return provider.newByteChannel(against(path), options, attrs);
  }

  /**
   * Deletes an entry that must be a file rather than a directory.
   *
   * @param path the entry to delete
   * @throws NotDirectoryException          never; declared for symmetry with the interface
   * @throws IOException                    if the entry names a directory, or cannot be deleted
   * @throws ClosedDirectoryStreamException if the stream is closed
   */
  @Override
  public synchronized void deleteFile (Path path)
    throws IOException {

    ensureOpen();

    Path resolvedPath = against(path);

    if (provider.readAttributes(resolvedPath, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isDirectory()) {
      throw new IOException("The path(" + resolvedPath + ") names a directory");
    }

    provider.delete(resolvedPath);
  }

  /**
   * Deletes an entry that must be a directory rather than a file.
   *
   * @param path the entry to delete
   * @throws NotDirectoryException          if the entry does not name a directory
   * @throws IOException                    if the entry cannot be deleted
   * @throws ClosedDirectoryStreamException if the stream is closed
   */
  @Override
  public synchronized void deleteDirectory (Path path)
    throws IOException {

    ensureOpen();

    Path resolvedPath = against(path);

    if (!provider.readAttributes(resolvedPath, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isDirectory()) {
      throw new NotDirectoryException(resolvedPath.toString());
    }

    provider.delete(resolvedPath);
  }

  /**
   * Moves an entry of this directory into another open directory stream.
   *
   * @param src        the entry to move, relative to this stream's directory
   * @param target     the stream whose directory will receive the entry
   * @param targetpath the name the entry will take, relative to the target stream's directory
   * @throws IOException                    if the move cannot be performed
   * @throws ClosedDirectoryStreamException if either stream is closed
   */
  @Override
  public synchronized void move (Path src, SecureDirectoryStream<Path> target, Path targetpath)
    throws IOException {

    ensureOpen();

    Path resolvedTarget;

    if (targetpath.isAbsolute()) {
      resolvedTarget = targetpath;
    } else if (target instanceof EphemeralDirectoryStream) {
      // resolved against the target stream's own directory, not against one of its entries
      resolvedTarget = ((EphemeralDirectoryStream)target).streamPath.resolve(targetpath);
    } else {
      throw new IOException("The target stream is not associated with this file system");
    }

    provider.move(against(src), resolvedTarget);
  }

  /**
   * Returns an attribute view of this stream's own directory.
   *
   * @param type the view type requested
   * @param <V>  the view type
   * @return the view, or {@code null} if the type is unsupported
   * @throws ClosedDirectoryStreamException if the stream is closed
   */
  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Class<V> type) {

    ensureOpen();

    return provider.getFileAttributeView(streamPath, type);
  }

  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    ensureOpen();

    return provider.getFileAttributeView(against(path), type, options);
  }
}
