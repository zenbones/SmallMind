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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Pair of connected input/output streams backed by a {@link CircularBuffer}.
 * Allows producer/consumer communication with optional external availability hints.
 */
public class CircularBufferIOStream implements Closeable {

  private static final int DEFAULT_SIZE = 1024;

  private final InputStream inputStream = new MarkableInputStream(new CircularBufferInputStream());
  private final OutputStream outputStream = new CircularBufferOutputStream();
  private final CircularBuffer circularBuffer;
  private final Supplier<Integer> availableSupplier;
  private boolean inputClosed = false;
  private boolean outputClosed = false;

  /**
   * Creates a stream with default buffer size and no availability supplier.
   */
  public CircularBufferIOStream () {

    this(DEFAULT_SIZE, null);
  }

  /**
   * Creates a stream with the given buffer size.
   *
   * @param size buffer capacity
   */
  public CircularBufferIOStream (int size) {

    this(size, null);
  }

  /**
   * Creates a stream with default buffer size and an availability supplier.
   *
   * @param availableSupplier optional supplier indicating readable bytes; {@code null} for unlimited
   */
  public CircularBufferIOStream (Supplier<Integer> availableSupplier) {

    this(DEFAULT_SIZE, availableSupplier);
  }

  /**
   * Creates a stream with the given buffer size and availability supplier.
   *
   * @param size              buffer capacity
   * @param availableSupplier optional supplier indicating readable bytes; {@code null} for unlimited
   */
  public CircularBufferIOStream (int size, Supplier<Integer> availableSupplier) {

    this.availableSupplier = availableSupplier;

    circularBuffer = new CircularBuffer(size);
  }

  /**
   * Closes the underlying buffer immediately.
   */
  @Override
  public void close () {

    circularBuffer.close();
  }

  /**
   * @return input stream end of the pipe
   */
  public InputStream asInputStream () {

    return inputStream;
  }

  /**
   * @return output stream end of the pipe
   */
  public OutputStream asOutputStream () {

    return outputStream;
  }

  public class CircularBufferInputStream extends InputStream {

    /**
     * Reads one byte or returns -1 on EOF.
     *
     * @throws IOException if closed or interrupted
     */
    @Override
    public synchronized int read ()
      throws IOException {

      if (inputClosed) {
        throw new SynchronousCloseException();
      } else {

        byte[] singleByte = new byte[1];

        try {
          circularBuffer.read(singleByte);

          return singleByte[0];
        } catch (InterruptedException interruptedException) {
          throw new IOException(interruptedException);
        }
      }
    }

    /**
     * Reads up to {@code len} bytes into {@code data} starting at {@code off}.
     *
     * @return bytes read (may block) or -1 on EOF
     * @throws IOException if closed or interrupted
     */
    @Override
    public synchronized int read (byte[] data, int off, int len)
      throws IOException {

      if (inputClosed) {
        throw new SynchronousCloseException();
      } else {
        try {

          return circularBuffer.read(data, off, len);
        } catch (InterruptedException interruptedException) {
          throw new IOException(interruptedException);
        }
      }
    }

    /**
     * Skips (discards) up to {@code n} bytes.
     *
     * @throws IOException if closed or interrupted
     */
    @Override
    public synchronized long skip (long n)
      throws IOException {

      if (inputClosed) {
        throw new SynchronousCloseException();
      } else {
        try {

          return circularBuffer.skip(n);
        } catch (InterruptedException interruptedException) {
          throw new IOException(interruptedException);
        }
      }
    }

    /**
     * Returns either a supplied available hint or {@link Integer#MAX_VALUE} when none is provided.
     */
    @Override
    public synchronized int available ()
      throws IOException {

      if (inputClosed) {
        throw new SynchronousCloseException();
      } else {

        Integer available;

        return ((availableSupplier == null) || ((available = availableSupplier.get()) == null)) ? Integer.MAX_VALUE : available;
      }
    }

    @Override
    public void mark (int readLimit) {

      throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void reset () {

      throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported () {

      return false;
    }

    /**
     * Closes the input side; closes the buffer when both ends are closed.
     */
    @Override
    public synchronized void close () {

      synchronized (CircularBufferIOStream.this) {
        if (!inputClosed) {
          inputClosed = true;

          if (outputClosed) {
            circularBuffer.close();
          }
        }
      }
    }
  }

  public class CircularBufferOutputStream extends OutputStream {

    /**
     * Writes a single byte to the buffer, blocking as needed.
     *
     * @throws IOException if closed or interrupted
     */
    @Override
    public synchronized void write (int singleByte)
      throws IOException {

      if (outputClosed) {
        throw new SynchronousCloseException();
      } else {
        try {
          circularBuffer.write(new byte[] {(byte)singleByte});
        } catch (InterruptedException interruptedException) {
          throw new IOException(interruptedException);
        }
      }
    }

    /**
     * Writes up to {@code len} bytes from {@code data} starting at {@code off}, blocking as needed.
     *
     * @throws IOException if closed or interrupted
     */
    @Override
    public synchronized void write (byte[] data, int off, int len)
      throws IOException {

      if (outputClosed) {
        throw new SynchronousCloseException();
      } else {
        try {
          circularBuffer.write(data, off, len);
        } catch (InterruptedException interruptedException) {
          throw new IOException(interruptedException);
        }
      }
    }

    /**
     * Closes the output side; closes the buffer when both ends are closed.
     */
    @Override
    public synchronized void close () {

      synchronized (CircularBufferIOStream.this) {
        if (!outputClosed) {
          outputClosed = true;

          if (inputClosed) {
            circularBuffer.close();
          }
        }
      }
    }
  }
}
