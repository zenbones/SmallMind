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
import java.nio.file.attribute.FileTime;
import org.smallmind.file.ephemeral.heap.FileNode;
import org.smallmind.file.ephemeral.heap.HeapEvent;
import org.smallmind.file.ephemeral.heap.HeapEventType;
import org.smallmind.nutsnbolts.io.ByteArrayIOStream;

/**
 * {@link SeekableByteChannel} backed by an in-memory {@link FileNode}. The channel
 * operates in either read-only or write-only mode, determined at construction time. All
 * public methods are {@code synchronized} to support concurrent access.
 *
 * <p>If the channel was opened with {@code deleteOnClose = true} the underlying file is
 * removed from the store when the channel is {@linkplain #close() closed}.
 */
public class EphemeralSeekableByteChannel implements SeekableByteChannel {

  private final EphemeralFileStore fileStore;
  private final FileNode fileNode;
  private final EphemeralPath filePath;
  private final ByteArrayIOStream stream;
  private final boolean read;
  private final boolean deleteOnClose;

  /**
   * Opens a channel over the given heap file node.
   *
   * @param fileStore     the owning {@link EphemeralFileStore} used for delete-on-close
   * @param fileNode      the heap node whose data this channel operates on
   * @param filePath      the logical path of the file, used when deleting on close
   * @param read          {@code true} to open the channel in read-only mode;
   *                      {@code false} for write-only mode
   * @param append        when {@code true} (and {@code read} is {@code false}) the write
   *                      position is advanced to end-of-stream before any writes occur
   * @param deleteOnClose when {@code true} the file is deleted from the store upon
   *                      {@link #close()}
   * @throws IOException if the underlying stream cannot be initialised
   */
  public EphemeralSeekableByteChannel (EphemeralFileStore fileStore, FileNode fileNode, EphemeralPath filePath, boolean read, boolean append, boolean deleteOnClose)
    throws IOException {

    this.fileStore = fileStore;
    this.filePath = filePath;
    this.fileNode = fileNode;
    this.read = read;
    this.deleteOnClose = deleteOnClose;

    stream = new ByteArrayIOStream(fileNode.getSegmentBuffer());
    if (append) {
      stream.asOutputStream().advance();
    }

    fileNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));
  }

  /**
   * Reads bytes from the channel into the given buffer. The last-access timestamp of the
   * underlying file is updated after a successful read.
   *
   * @param dst the buffer into which bytes are to be transferred
   * @return the number of bytes read, or {@code -1} if the channel has reached end-of-stream
   * @throws NonReadableChannelException if the channel was opened in write-only mode
   * @throws ClosedChannelException      if the channel has been closed
   * @throws IOException                 if an I/O error occurs
   */
  @Override
  public synchronized int read (ByteBuffer dst)
    throws IOException {

    if (!read) {
      throw new NonReadableChannelException();
    } else if (stream.isClosed()) {
      throw new ClosedChannelException();
    } else {

      byte[] buffer = new byte[dst.remaining()];
      int bytesRead = stream.asInputStream().read(buffer);

      if (bytesRead > 0) {
        dst.put(buffer, 0, bytesRead);

        fileNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));
      }

      return bytesRead;
    }
  }

  /**
   * Writes bytes from the given buffer into the channel. Both the last-access and
   * last-modified timestamps of the underlying file are updated after each write.
   *
   * @param src the buffer containing bytes to be written
   * @return the number of bytes written
   * @throws NonWritableChannelException if the channel was opened in read-only mode
   * @throws ClosedChannelException      if the channel has been closed
   * @throws IOException                 if an I/O error occurs
   */
  @Override
  public synchronized int write (ByteBuffer src)
    throws IOException {

    if (read) {
      throw new NonWritableChannelException();
    } else if (stream.isClosed()) {
      throw new ClosedChannelException();
    } else {

      byte[] buffer;
      int bytesWritten = src.remaining();

      if (bytesWritten > 0) {

        buffer = new byte[bytesWritten];
        src.get(buffer);
        stream.asOutputStream().write(buffer);

        fileNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));
        fileNode.getAttributes().setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));

        fileNode.bubble(new HeapEvent(this, filePath, HeapEventType.MODIFY));
      }

      return bytesWritten;
    }
  }

  /**
   * Returns the current byte position of this channel.
   *
   * @return the current position, measured in bytes from the beginning of the entity
   * @throws IOException if an I/O error occurs
   */
  @Override
  public synchronized long position ()
    throws IOException {

    synchronized (stream) {

      return (read) ? stream.asInputStream().position() : stream.asOutputStream().position();
    }
  }

  /**
   * Sets this channel's position to the given value.
   *
   * @param newPosition the new position; must be non-negative
   * @return this channel
   * @throws IOException if an I/O error occurs
   */
  @Override
  public synchronized SeekableByteChannel position (long newPosition)
    throws IOException {

    if (read) {
      stream.asInputStream().position(newPosition);
    } else {
      stream.asOutputStream().position(newPosition);
    }

    return this;
  }

  /**
   * Returns the current size of the entity to which this channel is connected. The
   * last-access timestamp is updated as a side effect.
   *
   * @return the current size in bytes
   * @throws IOException if an I/O error occurs
   */
  @Override
  public synchronized long size ()
    throws IOException {

    fileNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));

    return stream.size();
  }

  /**
   * Truncates the entity to which this channel is connected to the given size. If the given
   * size is greater than or equal to the current size the entity is left unchanged. Both the
   * last-access and last-modified timestamps are updated.
   *
   * @param size the new size; must be non-negative
   * @return this channel
   * @throws IOException if an I/O error occurs
   */
  @Override
  public synchronized SeekableByteChannel truncate (long size)
    throws IOException {

    stream.truncate(size);

    fileNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));
    fileNode.getAttributes().setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));

    fileNode.bubble(new HeapEvent(this, filePath, HeapEventType.MODIFY));

    return this;
  }

  /**
   * Indicates whether this channel is open.
   *
   * @return {@code true} if the channel has not been closed
   */
  @Override
  public synchronized boolean isOpen () {

    return !stream.isClosed();
  }

  /**
   * Closes this channel. If the channel was opened with {@code deleteOnClose = true} the
   * underlying file is removed from the store after the stream is closed.
   *
   * @throws IOException if the stream cannot be closed or, for delete-on-close channels,
   *                     if the file cannot be deleted
   */
  @Override
  public synchronized void close ()
    throws IOException {

    stream.close();
    if (deleteOnClose) {
      fileStore.delete(filePath);
    }
  }
}
