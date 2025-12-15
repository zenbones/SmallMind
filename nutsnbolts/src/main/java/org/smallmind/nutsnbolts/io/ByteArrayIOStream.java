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
import java.nio.charset.StandardCharsets;

/**
 * In-memory segmented stream supporting random read/write, truncation, and rewind semantics.
 * Provides paired input/output views that operate over shared backing segments.
 */
public class ByteArrayIOStream implements Closeable {

  private final ByteArrayInputStream inputStream = new ByteArrayInputStream();
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final ByteArrayIOBuffer segmentBuffer;
  private final ByteArrayIOBookmark readBookmark;
  private final ByteArrayIOBookmark writeBookmark;
  private boolean closed = false;

  /**
   * Constructs a stream with default 1KB segment allocation.
   */
  public ByteArrayIOStream () {

    this(1024);
  }

  /**
   * Constructs a stream with the given segment allocation size.
   *
   * @param allocation bytes per segment
   */
  public ByteArrayIOStream (int allocation) {

    segmentBuffer = new ByteArrayIOBuffer(allocation);
    readBookmark = new ByteArrayIOBookmark(allocation);
    writeBookmark = new ByteArrayIOBookmark(allocation);
  }

  /**
   * Constructs a stream backed by an existing buffer.
   *
   * @param segmentBuffer preexisting segmented buffer
   */
  public ByteArrayIOStream (ByteArrayIOBuffer segmentBuffer) {

    this.segmentBuffer = segmentBuffer;

    readBookmark = new ByteArrayIOBookmark(segmentBuffer.getAllocation());
    writeBookmark = new ByteArrayIOBookmark(segmentBuffer.getAllocation());
  }

  /**
   * @return {@code true} if the stream has been closed
   */
  public synchronized boolean isClosed () {

    return closed;
  }

  /**
   * Closes the stream; further operations will fail.
   */
  @Override
  public synchronized void close () {

    closed = true;
  }

  /**
   * @return current logical size of the stream in bytes
   * @throws IOException if the stream is closed
   */
  public synchronized long size ()
    throws IOException {

    if (closed) {
      throw new IOException("This stream has already been closed");
    } else {

      return segmentBuffer.getLimitBookmark().position();
    }
  }

  /**
   * Clears all data and resets bookmarks to the start.
   *
   * @throws IOException if the stream is closed
   */
  public synchronized void clear ()
    throws IOException {

    if (closed) {
      throw new IOException("This stream has already been closed");
    } else {
      segmentBuffer.clear();

      readBookmark.rewind();
      writeBookmark.rewind();
    }
  }

  /**
   * Truncates the stream to the specified size, zeroing remainder of the last segment.
   *
   * @param size new logical size
   * @throws IOException if closed
   */
  public synchronized void truncate (long size)
    throws IOException {

    if (closed) {
      throw new IOException("This stream has already been closed");
    } else {

      int truncatedSegmentIndex = (int)(size / segmentBuffer.getAllocation());
      int truncatedByteIndex = (int)(size % segmentBuffer.getAllocation());
      int truncatedSegmentCount = truncatedSegmentIndex + ((truncatedByteIndex) == 0 ? 0 : 1);

      while (segmentBuffer.getSegmentList().size() > truncatedSegmentCount) {
        segmentBuffer.getSegmentList().remove(segmentBuffer.getSegmentList().size() - 1);
      }

      if (truncatedByteIndex > 0) {

        byte[] segment = segmentBuffer.getSegmentList().get(truncatedSegmentIndex);

        for (int index = truncatedByteIndex; index < segmentBuffer.getAllocation(); index++) {
          segment[index] = 0;
        }
      }

      segmentBuffer.getLimitBookmark().position(size);
      if (readBookmark.position() > size) {
        readBookmark.position(size);
      }
      if (writeBookmark.position() > size) {
        writeBookmark.position(size);
      }
    }
  }

  /**
   * @return input-view of this stream
   */
  public ByteArrayInputStream asInputStream () {

    return inputStream;
  }

  /**
   * @return output-view of this stream
   */
  public ByteArrayOutputStream asOutputStream () {

    return outputStream;
  }

  /**
   * Converts buffered bytes to a UTF-8 string up to the logical end-of-stream.
   */
  public synchronized String toString () {

    StringBuilder builder = new StringBuilder();

    for (int index = 0; index < segmentBuffer.getSegmentList().size(); index++) {
      if (index == segmentBuffer.getLimitBookmark().segmentIndex()) {
        builder.append(new String(segmentBuffer.getSegmentList().get(index), 0, segmentBuffer.getLimitBookmark().byteIndex(), StandardCharsets.UTF_8));
      } else {
        builder.append(new String(segmentBuffer.getSegmentList().get(index), StandardCharsets.UTF_8));
      }
    }

    return builder.toString();
  }

  public class ByteArrayInputStream extends InputStream {

    private ByteArrayIOBookmark markBookmark;

    /**
     * @return current read position
     * @throws IOException if stream closed
     */
    public long position ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else {

          return readBookmark.position();
        }
      }
    }

    /**
     * Sets the read position, clamped to end-of-stream.
     *
     * @param position desired absolute position
     * @throws IOException if stream closed or position negative
     */
    public void position (long position)
      throws IOException {

      if (position < 0) {
        throw new IllegalArgumentException("Attempt to set position < 0");
      } else {
        synchronized (ByteArrayIOStream.this) {
          if (closed) {
            throw new IOException("This stream has already been closed");
          } else {
            readBookmark.position(Math.min(position, segmentBuffer.getLimitBookmark().position()));
          }
        }
      }
    }

    /**
     * Reads a byte at the given offset from the current position without advancing.
     *
     * @param index zero-based offset from current position
     * @return byte value
     * @throws IOException               if closed
     * @throws IndexOutOfBoundsException if index exceeds available bytes
     */
    public byte peek (int index)
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (index < 0 || index >= available()) {
          throw new IndexOutOfBoundsException(index + ">=" + available());
        } else {

          ByteArrayIOBookmark peekBookmark = readBookmark.offset(segmentBuffer.getLimitBookmark(), index);

          return segmentBuffer.getSegmentList().get(peekBookmark.segmentIndex())[peekBookmark.byteIndex()];
        }
      }
    }

    /**
     * Reads all available bytes into a new array.
     *
     * @return remaining bytes
     * @throws IOException if closed
     */
    public byte[] readAvailable ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else {

          byte[] buffer = new byte[available()];

          read(buffer);

          return buffer;
        }
      }
    }

    /**
     * Reads a single byte or returns -1 on end-of-stream.
     *
     * @throws IOException if closed
     */
    @Override
    public int read ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (available() == 0) {

          return -1;
        } else {

          byte singleByte;

          singleByte = segmentBuffer.getSegmentList().get(readBookmark.segmentIndex())[readBookmark.byteIndex()];
          readBookmark.inc(segmentBuffer.getLimitBookmark(), segmentBuffer.getSegmentList());

          return singleByte;
        }
      }
    }

    /**
     * Reads up to {@code len} bytes into {@code bytes} starting at {@code off}.
     *
     * @return number of bytes read or -1 on end-of-stream
     * @throws IOException if closed or bounds invalid
     */
    @Override
    public int read (byte[] bytes, int off, int len)
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (bytes == null) {
          throw new NullPointerException();
        } else if (off < 0 || len < 0 || off > bytes.length || len > bytes.length - off) {
          throw new IndexOutOfBoundsException();
        } else if (len == 0) {

          return 0;
        } else {

          int bytesAvailable;

          if ((bytesAvailable = available()) == 0) {

            return -1;
          } else {

            int bytesToRead = Math.min(bytesAvailable, len);

            for (int index = 0; index < bytesToRead; index++) {
              bytes[off + index] = segmentBuffer.getSegmentList().get(readBookmark.segmentIndex())[readBookmark.byteIndex()];
              readBookmark.inc(segmentBuffer.getLimitBookmark(), segmentBuffer.getSegmentList());
            }

            return bytesToRead;
          }
        }
      }
    }

    /**
     * Skips up to {@code n} bytes and returns the number actually skipped.
     *
     * @throws IOException if closed
     */
    @Override
    public long skip (long n)
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else {

          long bytesToSkip;

          if ((bytesToSkip = Math.min(available(), n)) > 0) {

            readBookmark.skip(segmentBuffer.getLimitBookmark(), bytesToSkip);

            return bytesToSkip;
          }

          return 0;
        }
      }
    }

    /**
     * @return number of bytes remaining to read
     * @throws IOException if closed
     */
    @Override
    public int available ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        return (int)(segmentBuffer.getLimitBookmark().position() - readBookmark.position());
      }
    }

    /**
     * Marks the current position for subsequent {@link #reset()}.
     */
    @Override
    public void mark (int readLimit) {

      synchronized (ByteArrayIOStream.this) {
        if (!closed) {
          markBookmark = new ByteArrayIOBookmark(readBookmark);
        }
      }
    }

    /**
     * Resets the read position to the last mark.
     *
     * @throws IOException if closed
     */
    @Override
    public void reset ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        readBookmark.reset(markBookmark);
      }
    }

    /**
     * Rewinds the read position to the beginning.
     *
     * @throws IOException if closed
     */
    public void rewind ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        readBookmark.rewind();
      }
    }

    /**
     * Closes the stream, clearing marks and closing the parent stream.
     */
    @Override
    public void close () {

      synchronized (ByteArrayIOStream.this) {
        markBookmark = null;

        ByteArrayIOStream.this.close();
      }
    }

    /**
     * @return {@code true}; marking is supported
     */
    @Override
    public boolean markSupported () {

      return true;
    }
  }

  public class ByteArrayOutputStream extends OutputStream {

    /**
     * @return current write position
     * @throws IOException if closed
     */
    public long position ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else {

          return writeBookmark.position();
        }
      }
    }

    /**
     * Sets the write position, clamped to the logical end.
     *
     * @param position desired position
     * @throws IOException if closed or position negative
     */
    public void position (long position)
      throws IOException {

      if (position < 0) {
        throw new IllegalArgumentException("Attempt to set position < 0");
      } else {
        synchronized (ByteArrayIOStream.this) {
          if (closed) {
            throw new IOException("This stream has already been closed");
          } else {
            writeBookmark.position(Math.min(position, segmentBuffer.getLimitBookmark().position()));
          }
        }
      }
    }

    /**
     * Writes a single byte, expanding segments as needed.
     *
     * @throws IOException if closed
     */
    @Override
    public void write (int b)
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        if (writeBookmark.segmentIndex() == segmentBuffer.getSegmentList().size()) {
          segmentBuffer.getSegmentList().add(new byte[segmentBuffer.getAllocation()]);
        }

        segmentBuffer.getSegmentList().get(writeBookmark.segmentIndex())[writeBookmark.byteIndex()] = (byte)b;

        if (segmentBuffer.getLimitBookmark().equals(writeBookmark)) {
          segmentBuffer.getLimitBookmark().inc(segmentBuffer.getLimitBookmark(), segmentBuffer.getSegmentList());
        }
        writeBookmark.inc(segmentBuffer.getLimitBookmark(), segmentBuffer.getSegmentList());
      }
    }

    /**
     * Writes {@code len} bytes from the array starting at {@code off}.
     *
     * @throws IOException if closed or bounds invalid
     */
    @Override
    public void write (byte[] bytes, int off, int len)
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (bytes == null) {
          throw new NullPointerException();
        } else if (off < 0 || len < 0 || off > bytes.length || len > bytes.length - off) {
          throw new IndexOutOfBoundsException();
        } else if (len > 0) {

          for (int index = off; index < len; index++) {
            write(bytes[index]);
          }
        }
      }
    }

    /**
     * Rewinds the write position to the beginning without truncating.
     *
     * @throws IOException if closed
     */
    public void rewind ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        writeBookmark.rewind();
      }
    }

    /**
     * Advances the write bookmark to the logical end-of-stream.
     *
     * @throws IOException if closed
     */
    public void advance ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        writeBookmark.reset(segmentBuffer.getLimitBookmark());
      }
    }

    /**
     * Closes the parent stream.
     */
    @Override
    public void close () {

      ByteArrayIOStream.this.close();
    }
  }
}
