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

/**
 * {@link InputStream} decorator that adds mark/reset support to any underlying stream by buffering
 * bytes read since the last {@link #mark(int)} call.
 */
public class MarkableInputStream extends InputStream {

  private final InputStream inputStream;
  private boolean closed = false;
  private byte[] buffer;
  private int readPos = -1;
  private int writePos = 0;

  /**
   * Constructs a markable stream wrapping the given delegate.
   *
   * @param inputStream the underlying stream to decorate
   */
  public MarkableInputStream (InputStream inputStream) {

    this.inputStream = inputStream;
  }

  /**
   * Reads a single byte, returning it from the mark buffer if a reset has occurred.
   *
   * @return the next byte as an unsigned int, or {@code -1} on end of stream
   * @throws IOException if the stream has been closed or reading fails
   */
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

  /**
   * Reads up to {@code b.length} bytes into the array.
   *
   * @param b destination array
   * @return number of bytes read, or {@code -1} on end of stream
   * @throws IOException if the stream has been closed or reading fails
   */
  @Override
  public int read (byte[] b)
    throws IOException {

    return read(b, 0, b.length);
  }

  /**
   * Reads up to {@code len} bytes into {@code b} at offset {@code off}, drawing first from the mark buffer
   * and then from the underlying stream.
   *
   * @param b   destination array
   * @param off starting index in {@code b}
   * @param len maximum bytes to read
   * @return total bytes read, or {@code -1} if end of stream is reached with no data
   * @throws IOException if the stream is closed or the offset/length are out of bounds
   */
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

  /**
   * Skips up to {@code n} bytes, capturing skipped data into the mark buffer when space remains.
   *
   * @param n maximum bytes to skip
   * @return number of bytes actually skipped
   * @throws IOException if the stream has been closed or skipping fails
   */
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

  /**
   * Returns an estimate of bytes that can be read without blocking, including any bytes held in the mark buffer.
   *
   * @return estimated available bytes
   * @throws IOException if the stream has been closed
   */
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

  /**
   * Closes this stream, releasing the mark buffer and closing the underlying stream.
   *
   * @throws IOException if closing the underlying stream fails
   */
  @Override
  public synchronized void close ()
    throws IOException {

    if (!closed) {
      closed = true;
      buffer = null;
      inputStream.close();
    }
  }

  /**
   * Records the current position so that a future {@link #reset()} can return to it, allocating an internal buffer
   * of up to {@code readLimit} bytes.
   *
   * @param readLimit the number of bytes that can be read before the mark is invalidated
   */
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

  /**
   * Repositions the stream to the location recorded by the most recent {@link #mark(int)} call.
   *
   * @throws IOException if no mark has been set, if the mark has been invalidated, or if the stream is closed
   */
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

  /**
   * Returns {@code true} because this stream always supports mark and reset.
   *
   * @return {@code true}
   */
  @Override
  public boolean markSupported () {

    return true;
  }
}
