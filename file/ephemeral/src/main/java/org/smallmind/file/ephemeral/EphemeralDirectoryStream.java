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

/**
 * {@link SecureDirectoryStream} implementation that iterates over entries in an ephemeral
 * heap directory. All mutating and navigation operations are guarded against use after
 * {@link #close()} by throwing {@link ClosedDirectoryStreamException}. Relative paths
 * supplied to the secure-stream methods are resolved against the stream's own path.
 */
public class EphemeralDirectoryStream implements SecureDirectoryStream<Path> {

  private final EphemeralFileSystemProvider provider;
  private final EphemeralPath streamPath;
  private final DirectoryNode directoryNode;
  private final DirectoryStream.Filter<? super Path> filter;
  private boolean closed = false;

  /**
   * Creates a directory stream for the specified heap directory node.
   *
   * @param provider      the file-system provider used to delegate new channel and stream operations
   * @param streamPath    the absolute path that this stream represents
   * @param directoryNode the heap node backing the directory
   * @param filter        an optional filter applied when iterating entries; may be {@code null}
   */
  public EphemeralDirectoryStream (EphemeralFileSystemProvider provider, EphemeralPath streamPath, DirectoryNode directoryNode, DirectoryStream.Filter<? super Path> filter) {

    this.provider = provider;
    this.streamPath = streamPath;
    this.directoryNode = directoryNode;
    this.filter = filter;
  }

  /**
   * Closes this directory stream. Subsequent operations on the stream will throw
   * {@link ClosedDirectoryStreamException}.
   */
  @Override
  public synchronized void close () {

    closed = true;
  }

  /**
   * Returns an iterator over the entries of this directory, applying the configured filter.
   *
   * @return an iterator of child {@link Path} objects
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized Iterator<Path> iterator () {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return directoryNode.iterator(streamPath, filter);
    }
  }

  /**
   * Opens a new directory stream for a sub-directory. A relative path is resolved against
   * this stream's own path before delegation.
   *
   * @param path    the sub-directory path (absolute or relative)
   * @param options link options (currently unused)
   * @return a new {@link SecureDirectoryStream} for the resolved path
   * @throws NoSuchFileException            if the resolved path does not exist
   * @throws NotDirectoryException          if the resolved path is not a directory
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized SecureDirectoryStream<Path> newDirectoryStream (Path path, LinkOption... options)
    throws NoSuchFileException, NotDirectoryException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return provider.newDirectoryStream(path.isAbsolute() ? path : streamPath.resolve(path), null, options);
    }
  }

  /**
   * Opens a seekable byte channel for a file entry. A relative path is resolved against
   * this stream's own path before delegation.
   *
   * @param path    the file path (absolute or relative)
   * @param options the open options controlling read/write semantics
   * @param attrs   optional file attributes to set on creation
   * @return the opened {@link SeekableByteChannel}
   * @throws IOException                    if the channel cannot be opened
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return provider.newByteChannel(path.isAbsolute() ? path : streamPath.resolve(path), options, attrs);
    }
  }

  /**
   * Deletes the file at the given path. A relative path is resolved against this stream's
   * own path before delegation.
   *
   * @param path the file path to delete (absolute or relative)
   * @throws IOException                    if the file cannot be deleted
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized void deleteFile (Path path)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.delete(path.isAbsolute() ? path : streamPath.resolve(path));
    }
  }

  /**
   * Deletes the directory at the given path. A relative path is resolved against this
   * stream's own path before delegation.
   *
   * @param path the directory path to delete (absolute or relative)
   * @throws IOException                    if the directory cannot be deleted
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized void deleteDirectory (Path path)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.delete(path.isAbsolute() ? path : streamPath.resolve(path));
    }
  }

  /**
   * Moves the entry at {@code src} to {@code targetpath} within the {@code target} stream.
   * Relative source and target paths are resolved against their respective stream paths.
   *
   * @param src        the source path (absolute or relative to this stream)
   * @param target     the destination directory stream
   * @param targetpath the destination path within the target stream (absolute or relative)
   * @throws IOException                    if the move cannot be performed
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized void move (Path src, SecureDirectoryStream<Path> target, Path targetpath)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.move(src.isAbsolute() ? src : streamPath.resolve(src), targetpath.isAbsolute() ? targetpath : target.iterator().next().resolve(targetpath));
    }
  }

  /**
   * Returns a file-attribute view for the directory itself. This implementation always
   * returns {@code null} because no view is associated with the stream's own directory entry.
   *
   * @param <V>  the view type
   * @param type the class of the desired view
   * @return always {@code null}
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Class<V> type) {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return null;
    }
  }

  /**
   * Returns a file-attribute view for the entry at the given path, delegating to the provider.
   * A relative path is resolved against this stream's own path before delegation.
   *
   * @param <V>     the view type
   * @param path    the entry path (absolute or relative)
   * @param type    the class of the desired view
   * @param options link options passed through to the provider
   * @return the requested view, or {@code null} when the view type is unsupported
   * @throws ClosedDirectoryStreamException if this stream has been closed
   */
  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return provider.getFileAttributeView(path, type, options);
    }
  }
}
