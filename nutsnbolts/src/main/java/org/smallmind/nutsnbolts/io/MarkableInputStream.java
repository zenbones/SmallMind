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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.InputStream;

public class MarkableInputStream extends InputStream {

  private final InputStream inputStream;
  private boolean closed = false;
  private byte[] buffer;
  private int readPos = -1;
  private int writePos = 0;

  public MarkableInputStream (InputStream inputStream) {

    this.inputStream = inputStream;
  }

  @Override
  public synchronized int read ()
    throws IOException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if ((buffer != null) && (readPos >= 0) && (readPos < writePos)) {

      return buffer[readPos++];
    } else {

      int anInt;

      if (((anInt = inputStream.read()) >= 0) && (buffer != null)) {
        if (writePos < buffer.length) {
          buffer[writePos++] = (byte)anInt;
        } else {
          buffer = null;
        }
      }

      return anInt;
    }
  }

  @Override
  public int read (byte[] b)
    throws IOException {

    return read(b, 0, b.length);
  }

  @Override
  public synchronized int read (byte[] b, int off, int len)
    throws IOException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if (b == null) {
      throw new NullPointerException();
    } else if ((off < 0) || (len < 0) || (off > b.length) || (len > b.length - off)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {

      return 0;
    } else {

      int bufferBytesRead = 0;
      int streamBytesRead = 0;

      if ((buffer != null) && (readPos >= 0) && (readPos < writePos)) {
        System.arraycopy(buffer, readPos, b, off, bufferBytesRead = Math.min(writePos - readPos, len));
        readPos += bufferBytesRead;
      }
      if (bufferBytesRead < len) {
        streamBytesRead = inputStream.read(b, off + bufferBytesRead, len - bufferBytesRead);
        if ((streamBytesRead > 0) && (buffer != null)) {
          if (writePos + streamBytesRead <= buffer.length) {
            System.arraycopy(b, off + bufferBytesRead, buffer, writePos, streamBytesRead);
            writePos += streamBytesRead;
          } else {
            buffer = null;
          }
        }
      }

      return bufferBytesRead + streamBytesRead;
    }
  }

  @Override
  public synchronized long skip (long n)
    throws IOException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if ((buffer != null) && (n < buffer.length - writePos)) {

      int streamBytesRead = inputStream.read(buffer, writePos, (int)n);

      writePos += streamBytesRead;

      return streamBytesRead;
    } else {
      buffer = null;

      return inputStream.skip(n);
    }
  }

  @Override
  public synchronized int available ()
    throws IOException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if ((buffer == null) || (readPos < 0)) {

      return inputStream.available();
    } else {

      int remaining = writePos - readPos;
      int available = inputStream.available();

      return available < Integer.MAX_VALUE - remaining ? available + remaining : Integer.MAX_VALUE;
    }
  }

  @Override
  public synchronized void close ()
    throws IOException {

    if (!closed) {
      closed = true;
      buffer = null;
      inputStream.close();
    }
  }

  @Override
  public synchronized void mark (int readLimit) {

    if (!closed) {
      if ((buffer == null) || (readPos < 0)) {
        buffer = new byte[readLimit];
        readPos = -1;
        writePos = 0;
      } else {

        byte[] exchangedBuffer = new byte[Math.max(writePos - readPos, readLimit)];

        System.arraycopy(buffer, readPos, exchangedBuffer, 0, writePos - readPos);
        buffer = exchangedBuffer;
        writePos -= readPos;
        readPos = 0;
      }
    }
  }

  @Override
  public synchronized void reset ()
    throws IOException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if (buffer == null) {
      throw new IOException("There is no valid mark on this stream");
    }

    readPos = 0;
  }

  @Override
  public boolean markSupported () {

    return true;
  }
}
