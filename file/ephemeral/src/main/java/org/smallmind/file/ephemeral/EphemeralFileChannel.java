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
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.smallmind.file.ephemeral.heap.FileNode;

/**
 * {@link FileChannel} over the content of an ephemeral file.
 *
 * <p>A {@code FileChannel} is required for more than symmetry: {@link FileChannel#open} and
 * {@code Files.newByteChannel} are distinct entry points, and a great deal of ordinary code reaches
 * for the former. Without this class every such caller fails with an
 * {@link UnsupportedOperationException} no matter how complete the rest of the file system is.
 *
 * <p>Like {@link EphemeralSeekableByteChannel}, this channel owns only its position; the bytes live
 * in the file's {@link org.smallmind.file.ephemeral.heap.HeapFileContent} and are addressed
 * absolutely, which is what makes the positional {@code read} and {@code write} overloads
 * straightforward and leaves concurrent channels independent.
 *
 * <p>Memory mapping is not supported. There is no contiguous array to hand out — content is held in
 * segments — and a {@link MappedByteBuffer} cannot be synthesised without one.
 */
public class EphemeralFileChannel extends FileChannel {

  private final EphemeralFileStore fileStore;
  private final FileNode fileNode;
  private final EphemeralPath filePath;
  private final boolean readable;
  private final boolean writable;
  private final boolean append;
  private final boolean deleteOnClose;
  private long position;

  /**
   * Creates a channel over a file.
   *
   * @param fileStore the store owning the file, notified of modifications
   * @param fileNode  the file this channel addresses
   * @param filePath  the absolute path of the file, used when reporting changes
   * @param options   the validated open options governing this channel
   */
  EphemeralFileChannel (EphemeralFileStore fileStore, FileNode fileNode, EphemeralPath filePath, EphemeralFileStore.OpenOptions options) {

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

    if (!isOpen()) {
      throw new ClosedChannelException();
    }
  }

  /**
   * Reads into a buffer from an absolute position, without moving this channel's position.
   *
   * @param dst          the buffer to read into
   * @param readPosition the absolute position to read from
   * @return the number of bytes read, or {@code -1} at end of file
   * @throws IOException if the channel is closed
   */
  private int readAt (ByteBuffer dst, long readPosition)
    throws IOException {

    if (!readable) {
      throw new NonReadableChannelException();
    } else {

      byte[] buffer = new byte[dst.remaining()];
      int bytesRead = fileNode.getContent().read(readPosition, buffer, 0, buffer.length);

      if (bytesRead > 0) {
        dst.put(buffer, 0, bytesRead);
        fileStore.reportAccessed(fileNode);
      }

      return bytesRead;
    }
  }

  /**
   * Writes from a buffer at an absolute position, without moving this channel's position.
   *
   * @param src           the buffer to write from
   * @param writePosition the absolute position to write at
   * @return the number of bytes written
   * @throws IOException if the channel is closed, or the store is out of space
   */
  private int writeAt (ByteBuffer src, long writePosition)
    throws IOException {

    if (!writable) {
      throw new NonWritableChannelException();
    } else {

      int bytesWritten = src.remaining();

      if (bytesWritten > 0) {

        byte[] buffer = new byte[bytesWritten];

        src.get(buffer);
        fileNode.getContent().write(writePosition, buffer, 0, bytesWritten);
        fileStore.reportModified(fileNode, filePath);
      }

      return bytesWritten;
    }
  }

  @Override
  public synchronized int read (ByteBuffer dst)
    throws IOException {

    ensureOpen();

    int bytesRead = readAt(dst, position);

    if (bytesRead > 0) {
      position += bytesRead;
    }

    return bytesRead;
  }

  @Override
  public synchronized long read (ByteBuffer[] dsts, int offset, int length)
    throws IOException {

    ensureOpen();

    long total = 0;

    for (int index = offset; index < (offset + length); index++) {

      int bytesRead = read(dsts[index]);

      if (bytesRead < 0) {

        return (total == 0) ? -1 : total;
      }

      total += bytesRead;
    }

    return total;
  }

  @Override
  public synchronized int write (ByteBuffer src)
    throws IOException {

    ensureOpen();

    if (!writable) {
      throw new NonWritableChannelException();
    } else if (append) {

      int bytesWritten = src.remaining();

      if (bytesWritten > 0) {

        byte[] buffer = new byte[bytesWritten];

        src.get(buffer);
        // resolving the end and writing must be one atomic step, or two appenders collide
        position = fileNode.getContent().append(buffer, 0, bytesWritten);
        fileStore.reportModified(fileNode, filePath);
      }

      return bytesWritten;
    } else {

      int bytesWritten = writeAt(src, position);

      position += bytesWritten;

      return bytesWritten;
    }
  }

  @Override
  public synchronized long write (ByteBuffer[] srcs, int offset, int length)
    throws IOException {

    ensureOpen();

    long total = 0;

    for (int index = offset; index < (offset + length); index++) {
      total += write(srcs[index]);
    }

    return total;
  }

  @Override
  public synchronized long position ()
    throws IOException {

    ensureOpen();

    return position;
  }

  @Override
  public synchronized FileChannel position (long newPosition)
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
  public synchronized FileChannel truncate (long size)
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

  /**
   * Does nothing. All content is already held in memory, so there is nothing to flush.
   *
   * @param metaData ignored
   * @throws IOException if the channel is closed
   */
  @Override
  public void force (boolean metaData)
    throws IOException {

    ensureOpen();
  }

  @Override
  public synchronized long transferTo (long position, long count, WritableByteChannel target)
    throws IOException {

    ensureOpen();

    if ((position < 0) || (count < 0)) {
      throw new IllegalArgumentException("Negative position or count");
    } else {

      long transferred = 0;

      while (transferred < count) {

        ByteBuffer buffer = ByteBuffer.allocate((int)Math.min(count - transferred, 8192));
        int bytesRead = readAt(buffer, position + transferred);

        if (bytesRead <= 0) {
          break;
        }

        buffer.flip();
        transferred += target.write(buffer);

        if (buffer.hasRemaining()) {
          // the destination stopped accepting bytes
          break;
        }
      }

      return transferred;
    }
  }

  @Override
  public synchronized long transferFrom (ReadableByteChannel src, long position, long count)
    throws IOException {

    ensureOpen();

    if ((position < 0) || (count < 0)) {
      throw new IllegalArgumentException("Negative position or count");
    } else {

      long transferred = 0;

      while (transferred < count) {

        ByteBuffer buffer = ByteBuffer.allocate((int)Math.min(count - transferred, 8192));

        if (src.read(buffer) < 0) {
          break;
        }

        buffer.flip();

        int remaining = buffer.remaining();

        if (remaining == 0) {
          break;
        }

        writeAt(buffer, position + transferred);
        transferred += remaining;
      }

      return transferred;
    }
  }

  @Override
  public synchronized int read (ByteBuffer dst, long position)
    throws IOException {

    ensureOpen();

    if (position < 0) {
      throw new IllegalArgumentException("Negative position");
    }

    return readAt(dst, position);
  }

  @Override
  public synchronized int write (ByteBuffer src, long position)
    throws IOException {

    ensureOpen();

    if (position < 0) {
      throw new IllegalArgumentException("Negative position");
    }

    return writeAt(src, position);
  }

  /**
   * Always fails. Content is held as a list of segments rather than one contiguous array, so no
   * {@link MappedByteBuffer} can be produced over it.
   *
   * @param mode     ignored
   * @param position ignored
   * @param size     ignored
   * @return never returns
   * @throws UnsupportedOperationException always
   */
  @Override
  public MappedByteBuffer map (MapMode mode, long position, long size) {

    throw new UnsupportedOperationException("An in-memory file cannot be memory mapped");
  }

  /**
   * Acquires a lock on this file. Since the heap is private to one JVM there is nothing to arbitrate
   * with, so the returned lock is valid but has no effect beyond its own bookkeeping.
   *
   * @param position the position the lock begins at
   * @param size     the number of bytes locked
   * @param shared   whether the lock is shared
   * @return a valid lock over the requested region
   * @throws IOException if the channel is closed
   */
  @Override
  public FileLock lock (long position, long size, boolean shared)
    throws IOException {

    ensureOpen();

    return new EphemeralFileLock(this, position, size, shared);
  }

  /**
   * Attempts to acquire a lock on this file, which always succeeds for a single-JVM heap.
   *
   * @param position the position the lock begins at
   * @param size     the number of bytes locked
   * @param shared   whether the lock is shared
   * @return a valid lock over the requested region
   * @throws IOException if the channel is closed
   */
  @Override
  public FileLock tryLock (long position, long size, boolean shared)
    throws IOException {

    return lock(position, size, shared);
  }

  /**
   * Releases this channel, deleting the file if it was opened with
   * {@link java.nio.file.StandardOpenOption#DELETE_ON_CLOSE}. A file that has already been removed
   * by other means is not an error.
   */
  @Override
  protected void implCloseChannel () {

    if (deleteOnClose) {
      try {
        fileStore.delete(filePath);
      } catch (IOException ioException) {
        // the file is already gone, which is the outcome this option asked for
      }
    }
  }

  /**
   * The {@link FileLock} handed out by {@link EphemeralFileChannel#lock(long, long, boolean)}.
   */
  private static class EphemeralFileLock extends FileLock {

    private boolean valid = true;

    private EphemeralFileLock (FileChannel channel, long position, long size, boolean shared) {

      super(channel, position, size, shared);
    }

    @Override
    public synchronized boolean isValid () {

      return valid && channel().isOpen();
    }

    @Override
    public synchronized void release () {

      valid = false;
    }
  }
}
