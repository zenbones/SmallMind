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
import java.nio.channels.AsynchronousCloseException;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Thread-safe fixed-capacity circular byte buffer that blocks producers and consumers until space or data is available,
 * with optional per-operation timeouts.
 */
public class CircularBuffer implements Closeable {

  private enum Operation {

    READ, WRITE
  }

  private Operation lastOperation = Operation.READ;
  private boolean closed = false;
  private int readPos = 0;
  private int writePos = 0;
  private byte[] buffer;

  /**
   * Creates a buffer with the specified capacity.
   *
   * @param size total byte capacity of the circular buffer
   */
  public CircularBuffer (int size) {

    buffer = new byte[size];
  }

  /**
   * Returns whether this buffer has been closed.
   *
   * @return {@code true} if {@link #close()} has been called
   */
  public synchronized boolean isClosed () {

    return closed;
  }

  /**
   * Closes the buffer and wakes all threads blocked in read, write, or skip calls.
   */
  public synchronized void close () {

    if (!closed) {
      closed = true;
      buffer = null;

      notifyAll();
    }
  }

  /**
   * Returns the number of bytes currently available to read without blocking.
   *
   * @return bytes ready to be consumed
   * @throws IOException if the buffer has been closed
   */
  public synchronized int readAvailable ()
    throws IOException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if (readPos < writePos) {

      return writePos - readPos;
    } else if (readPos > writePos) {

      return (buffer.length - readPos) + writePos;
    } else {

      return Operation.WRITE.equals(lastOperation) ? buffer.length : 0;
    }
  }

  /**
   * Returns the number of bytes that can be written without blocking.
   *
   * @return free bytes in the buffer
   * @throws IOException if the buffer has been closed
   */
  public synchronized int writeAvailable ()
    throws IOException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if (writePos < readPos) {

      return readPos - writePos;
    } else if (writePos > readPos) {

      return (buffer.length - writePos) + readPos;
    } else {

      return Operation.READ.equals(lastOperation) ? buffer.length : 0;
    }
  }

  /**
   * Reads exactly {@code data.length} bytes into the array, blocking indefinitely until all bytes are available.
   *
   * @param data destination array to fill
   * @return number of bytes read (equal to {@code data.length})
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public int read (byte[] data)
    throws IOException, InterruptedException {

    return read(data, 0, data.length, 0);
  }

  /**
   * Reads up to {@code data.length} bytes, waiting at most {@code timeout} milliseconds for data.
   *
   * @param data    destination array
   * @param timeout maximum wait in milliseconds between availability checks; 0 means wait indefinitely
   * @return number of bytes read, which may be less than {@code data.length} if the timeout elapses
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public int read (byte[] data, long timeout)
    throws IOException, InterruptedException {

    return read(data, 0, data.length, timeout);
  }

  /**
   * Reads up to {@code len} bytes into {@code data} starting at {@code off}, blocking indefinitely as needed.
   *
   * @param data destination array
   * @param off  starting index in {@code data}
   * @param len  maximum bytes to read
   * @return bytes read
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if interrupted while waiting
   */
  public int read (byte[] data, int off, int len)
    throws IOException, InterruptedException {

    return read(data, off, len, 0);
  }

  /**
   * Reads up to {@code len} bytes into {@code data} starting at {@code off}, waiting at most {@code timeout} milliseconds.
   *
   * @param data    destination array
   * @param off     starting index in {@code data}
   * @param len     maximum bytes to read
   * @param timeout maximum wait in milliseconds; 0 means wait indefinitely
   * @return bytes read, which may be less than {@code len} if the timeout elapses first
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public int read (byte[] data, int off, int len, long timeout)
    throws IOException, InterruptedException {

    return transfer(data, off, len, timeout, Operation.READ);
  }

  /**
   * Writes all bytes from {@code data} into the buffer, blocking indefinitely until space is available.
   *
   * @param data source array to write
   * @return number of bytes written (equal to {@code data.length})
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public int write (byte[] data)
    throws IOException, InterruptedException {

    return write(data, 0, data.length, 0);
  }

  /**
   * Writes up to {@code data.length} bytes, waiting at most {@code timeout} milliseconds for space.
   *
   * @param data    source array
   * @param timeout maximum wait in milliseconds; 0 means wait indefinitely
   * @return bytes written, which may be less than {@code data.length} if the timeout elapses
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public int write (byte[] data, long timeout)
    throws IOException, InterruptedException {

    return write(data, 0, data.length, timeout);
  }

  /**
   * Writes up to {@code len} bytes from {@code data} starting at {@code off}, blocking indefinitely as needed.
   *
   * @param data source array
   * @param off  starting index in {@code data}
   * @param len  maximum bytes to write
   * @return bytes written
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if interrupted while waiting
   */
  public int write (byte[] data, int off, int len)
    throws IOException, InterruptedException {

    return write(data, off, len, 0);
  }

  /**
   * Writes up to {@code len} bytes from {@code data} starting at {@code off}, waiting at most {@code timeout} milliseconds.
   *
   * @param data    source array
   * @param off     starting index in {@code data}
   * @param len     maximum bytes to write
   * @param timeout maximum wait in milliseconds; 0 means wait indefinitely
   * @return bytes written, which may be less than {@code len} if the timeout elapses first
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public int write (byte[] data, int off, int len, long timeout)
    throws IOException, InterruptedException {

    return transfer(data, off, len, timeout, Operation.WRITE);
  }

  /**
   * Discards up to {@code len} bytes from the buffer, blocking indefinitely as needed.
   *
   * @param len number of bytes to discard
   * @return number of bytes actually discarded
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public long skip (long len)
    throws IOException, InterruptedException {

    return skip(len, 0);
  }

  /**
   * Discards up to {@code len} bytes from the buffer, waiting at most {@code timeout} milliseconds.
   *
   * @param len     number of bytes to discard
   * @param timeout maximum wait in milliseconds; 0 means wait indefinitely
   * @return number of bytes actually discarded, which may be less than {@code len} if timeout elapses
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public synchronized long skip (long len, long timeout)
    throws IOException, InterruptedException {

    if (closed) {
      throw new SynchronousCloseException();
    } else {

      long millisWaited = 0;
      long totalBytesSkipped = 0;

      do {
        if ((readPos != writePos) || (!Operation.READ.equals(lastOperation))) {

          long bytesSkipped = Math.min((readPos < writePos ? writePos : buffer.length) - readPos, len - totalBytesSkipped);

          if ((readPos += bytesSkipped) == buffer.length) {
            readPos = 0;
          }
          lastOperation = Operation.READ;
          totalBytesSkipped += bytesSkipped;

          notifyAll();
        } else {

          long start = System.currentTimeMillis();

          wait(timeout);

          if (closed) {
            throw new AsynchronousCloseException();
          }

          millisWaited += System.currentTimeMillis() - start;
        }
      } while ((totalBytesSkipped < len) && ((timeout == 0) || (millisWaited < timeout)));

      return totalBytesSkipped;
    }
  }

  /**
   * Performs a read or write transfer on the circular buffer, blocking and retrying until the requested bytes
   * are transferred or the timeout elapses.
   *
   * @param data      the byte array to read into or write from
   * @param off       starting offset in {@code data}
   * @param len       maximum bytes to transfer
   * @param timeout   maximum wait in milliseconds; 0 means indefinite
   * @param operation whether to READ from or WRITE to the buffer
   * @return number of bytes actually transferred
   * @throws IOException          if the buffer has been closed
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  private synchronized int transfer (byte[] data, int off, int len, long timeout, Operation operation)
    throws IOException, InterruptedException {

    if (closed) {
      throw new SynchronousCloseException();
    } else if (data == null) {
      throw new NullPointerException();
    } else if ((off < 0) || (len < 0) || (off > data.length) || (len > data.length - off)) {
      throw new IndexOutOfBoundsException();
    } else if (len == 0) {

      return 0;
    } else {

      long millisWaited = 0;
      int totalBytesTransferred = 0;

      do {
        if ((readPos != writePos) || (!operation.equals(lastOperation))) {

          int bytesTransferred;

          switch (operation) {
            case READ:
              System.arraycopy(buffer, readPos, data, off + totalBytesTransferred, bytesTransferred = Math.min((readPos < writePos ? writePos : buffer.length) - readPos, len - totalBytesTransferred));
              if ((readPos += bytesTransferred) == buffer.length) {
                readPos = 0;
              }
              break;
            case WRITE:
              System.arraycopy(data, off + totalBytesTransferred, buffer, writePos, bytesTransferred = Math.min((writePos < readPos ? readPos : buffer.length) - writePos, len - totalBytesTransferred));
              if ((writePos += bytesTransferred) == buffer.length) {
                writePos = 0;
              }
              break;
            default:
              throw new UnknownSwitchCaseException(operation.name());
          }

          lastOperation = operation;
          totalBytesTransferred += bytesTransferred;

          notifyAll();
        } else {

          long start = System.currentTimeMillis();

          wait(timeout);

          if (closed) {
            throw new AsynchronousCloseException();
          }

          millisWaited += System.currentTimeMillis() - start;
        }
      } while ((totalBytesTransferred < len) && ((timeout == 0) || (millisWaited < timeout)));

      return totalBytesTransferred;
    }
  }
}
