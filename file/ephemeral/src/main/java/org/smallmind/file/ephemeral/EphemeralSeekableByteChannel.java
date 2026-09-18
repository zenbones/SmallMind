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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
import org.smallmind.file.ephemeral.heap.FileNode;

/**
 * {@link SeekableByteChannel} over the content of an ephemeral file.
 *
 * <p>The channel owns its position and nothing else; the bytes live in the file's
 * {@link org.smallmind.file.ephemeral.heap.HeapFileContent}, which is addressed absolutely. Two
 * channels open on one file therefore advance independently, a single channel can be opened for
 * reading and writing at once, and two appending channels cannot overwrite each other — each
 * resolves the end of the file at the moment it writes.
 */
public class EphemeralSeekableByteChannel implements SeekableByteChannel {

  private final EphemeralFileStore fileStore;
  private final FileNode fileNode;
  private final EphemeralPath filePath;
  private final boolean readable;
  private final boolean writable;
  private final boolean append;
  private final boolean deleteOnClose;
  private long position;
  private boolean closed;

  /**
   * Creates a channel over a file.
   *
   * @param fileStore the store owning the file, notified of modifications
   * @param fileNode  the file this channel addresses
   * @param filePath  the absolute path of the file, used when reporting changes
   * @param options   the validated open options governing this channel
   */
  EphemeralSeekableByteChannel (EphemeralFileStore fileStore, FileNode fileNode, EphemeralPath filePath, EphemeralFileStore.OpenOptions options) {

    this.fileStore = fileStore;
    this.fileNode = fileNode;
    this.filePath = filePath;

    readable = options.isRead();
    writable = options.isWrite();
    append = options.isAppend();
    deleteOnClose = options.isDeleteOnClose();

    fileStore.reportAccessed(fileNode);
  }

  /**
   * Throws if this channel has been closed.
   *
   * @throws ClosedChannelException if the channel is closed
   */
  private void ensureOpen ()
    throws ClosedChannelException {

    if (closed) {
      throw new ClosedChannelException();
    }
  }

  @Override
  public synchronized int read (ByteBuffer dst)
    throws IOException {

    ensureOpen();

    if (!readable) {
      throw new NonReadableChannelException();
    } else {

      byte[] buffer = new byte[dst.remaining()];
      int bytesRead = fileNode.getContent().read(position, buffer, 0, buffer.length);

      if (bytesRead > 0) {
        dst.put(buffer, 0, bytesRead);
        position += bytesRead;
        fileStore.reportAccessed(fileNode);
      }

      return bytesRead;
    }
  }

  @Override
  public synchronized int write (ByteBuffer src)
    throws IOException {

    ensureOpen();

    if (!writable) {
      throw new NonWritableChannelException();
    } else {

      int bytesWritten = src.remaining();

      if (bytesWritten > 0) {

        byte[] buffer = new byte[bytesWritten];

        src.get(buffer);

        // an appending channel resolves the end of the file and writes atomically, so that two
        // appenders interleave their writes rather than overwriting one another
        if (append) {
          position = fileNode.getContent().append(buffer, 0, bytesWritten);
        } else {
          fileNode.getContent().write(position, buffer, 0, bytesWritten);
          position += bytesWritten;
        }

        fileStore.reportModified(fileNode, filePath);
      }

      return bytesWritten;
    }
  }

  @Override
  public synchronized long position ()
    throws IOException {

    ensureOpen();

    return position;
  }

  @Override
  public synchronized SeekableByteChannel position (long newPosition)
    throws IOException {

    ensureOpen();

    if (newPosition < 0) {
      throw new IllegalArgumentException("Negative position");
    }

    position = newPosition;

    return this;
  }

  @Override
  public synchronized long size ()
    throws IOException {

    ensureOpen();

    return fileNode.getContent().size();
  }

  @Override
  public synchronized SeekableByteChannel truncate (long size)
    throws IOException {

    ensureOpen();

    if (!writable) {
      throw new NonWritableChannelException();
    } else if (size < 0) {
      throw new IllegalArgumentException("Negative size");
    } else {
      if (size < fileNode.getContent().size()) {
        fileNode.getContent().truncate(size);
        fileStore.reportModified(fileNode, filePath);
      }
      if (position > size) {
        position = size;
      }

      return this;
    }
  }

  @Override
  public synchronized boolean isOpen () {

    return !closed;
  }

  /**
   * Closes this channel, deleting the file if it was opened with
   * {@link java.nio.file.StandardOpenOption#DELETE_ON_CLOSE}. Closing an already closed channel
   * has no effect, so the deletion happens at most once, and a file that has already been removed
   * by other means is not an error.
   */
  @Override
  public synchronized void close () {

    if (!closed) {
      closed = true;
      fileStore.unregisterOpenResource(this);

      if (deleteOnClose) {
        try {
          fileStore.delete(filePath);
        } catch (IOException | ClosedFileSystemException exception) {
          // the file, or the whole file system, is already gone - either way there is nothing
          // left to delete
        }
      }
    }
  }
}
