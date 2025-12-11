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
import org.smallmind.nutsnbolts.io.ByteArrayIOStream;

public class EphemeralSeekableByteChannel implements SeekableByteChannel {

  private final EphemeralFileStore fileStore;
  private final FileNode fileNode;
  private final EphemeralPath filePath;
  private final ByteArrayIOStream stream;
  private final boolean read;
  private final boolean deleteOnClose;

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
      }

      return bytesWritten;
    }
  }

  @Override
  public synchronized long position ()
    throws IOException {

    synchronized (stream) {

      return (read) ? stream.asInputStream().position() : stream.asOutputStream().position();
    }
  }

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

  @Override
  public synchronized long size ()
    throws IOException {

    fileNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));

    return stream.size();
  }

  @Override
  public synchronized SeekableByteChannel truncate (long size)
    throws IOException {

    stream.truncate(size);

    fileNode.getAttributes().setLastAccessTime(FileTime.fromMillis(System.currentTimeMillis()));
    fileNode.getAttributes().setLastModifiedTime(FileTime.fromMillis(System.currentTimeMillis()));

    return this;
  }

  @Override
  public synchronized boolean isOpen () {

    return !stream.isClosed();
  }

  @Override
  public synchronized void close ()
    throws IOException {

    stream.close();
    if (deleteOnClose) {
      fileStore.delete(filePath);
    }
  }
}
