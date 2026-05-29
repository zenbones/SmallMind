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
 * In-memory stream backed by a segmented {@link ByteArrayIOBuffer} that exposes independent
 * input and output views, supports random positioning, truncation, and mark/reset semantics.
 */
public class ByteArrayIOStream implements Closeable {

  private final ByteArrayInputStream inputStream = new ByteArrayInputStream();
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final ByteArrayIOBuffer segmentBuffer;
  private final ByteArrayIOBookmark readBookmark;
  private final ByteArrayIOBookmark writeBookmark;
  private boolean closed = false;

  /**
   * Constructs a stream with a default 1 024-byte segment allocation.
   */
  public ByteArrayIOStream () {

    this(1024);
  }

  /**
   * Constructs an empty stream using segments of the specified byte capacity.
   *
   * @param allocation number of bytes per segment
   */
  public ByteArrayIOStream (int allocation) {

    segmentBuffer = new ByteArrayIOBuffer(allocation);
    readBookmark = new ByteArrayIOBookmark(allocation);
    writeBookmark = new ByteArrayIOBookmark(allocation);
  }

  /**
   * Constructs a stream that operates over an already-populated {@link ByteArrayIOBuffer}.
   *
   * @param segmentBuffer existing buffer to use as the backing store
   */
  public ByteArrayIOStream (ByteArrayIOBuffer segmentBuffer) {

    this.segmentBuffer = segmentBuffer;

    readBookmark = new ByteArrayIOBookmark(segmentBuffer.getAllocation());
    writeBookmark = new ByteArrayIOBookmark(segmentBuffer.getAllocation());
  }

  /**
   * Returns whether this stream has been closed.
   *
   * @return {@code true} if {@link #close()} has been called
   */
  public synchronized boolean isClosed () {

    return closed;
  }

  /**
   * Marks this stream as closed; subsequent operations on either the input or output view will throw.
   */
  @Override
  public synchronized void close () {

    closed = true;
  }

  /**
   * Returns the number of bytes currently written to this stream.
   *
   * @return logical byte count
   * @throws IOException if the stream has been closed
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
   * Discards all data and resets the read and write positions to zero.
   *
   * @throws IOException if the stream has been closed
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
   * Reduces the logical size of the stream to {@code size} bytes, zeroing any trailing bytes in the last segment
   * and clamping read/write positions that would fall beyond the new end.
   *
   * @param size the new logical size in bytes
   * @throws IOException if the stream has been closed
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
   * Returns the input view for reading from this stream.
   *
   * @return the shared input stream
   */
  public ByteArrayInputStream asInputStream () {

    return inputStream;
  }

  /**
   * Returns the output view for writing to this stream.
   *
   * @return the shared output stream
   */
  public ByteArrayOutputStream asOutputStream () {

    return outputStream;
  }

  /**
   * Returns the UTF-8 string formed by all bytes written up to the current logical end of stream.
   *
   * @return string representation of the buffered content
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

  /**
   * Input view into the parent {@link ByteArrayIOStream}, supporting random access, peek, mark, and rewind.
   */
  public class ByteArrayInputStream extends InputStream {

    private ByteArrayIOBookmark markBookmark;

    /**
     * Returns the current read position in the stream.
     *
     * @return absolute byte offset of the next read
     * @throws IOException if the stream has been closed
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
     * Moves the read position to the given absolute offset, clamping to the logical end of the stream.
     *
     * @param position desired absolute byte offset
     * @throws IOException              if the stream has been closed
     * @throws IllegalArgumentException if {@code position} is negative
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
     * Returns the byte at {@code index} positions ahead of the current read position without consuming it.
     *
     * @param index zero-based lookahead distance from the current position
     * @return the byte at that position
     * @throws IOException               if the stream has been closed
     * @throws IndexOutOfBoundsException if {@code index} is negative or beyond the available bytes
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
     * Reads all bytes currently available from the stream into a new array.
     *
     * @return a byte array containing all remaining readable bytes
     * @throws IOException if the stream has been closed
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
     * Reads the next byte or returns {@code -1} when no data is available.
     *
     * @return the next byte as an unsigned int, or {@code -1} on end of stream
     * @throws IOException if the stream has been closed
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
     * Reads up to {@code len} bytes into the supplied array starting at offset {@code off}.
     *
     * @param bytes destination array
     * @param off   starting index in {@code bytes}
     * @param len   maximum number of bytes to read
     * @return number of bytes actually read, or {@code -1} on end of stream
     * @throws IOException if the stream is closed or the offset/length are out of bounds
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
     * Advances the read position by up to {@code n} bytes and returns the count actually skipped.
     *
     * @param n maximum number of bytes to skip
     * @return number of bytes skipped, which may be less than {@code n} if the stream ends
     * @throws IOException if the stream has been closed
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
     * Returns the number of bytes between the current read position and the logical end of stream.
     *
     * @return bytes available without blocking
     * @throws IOException if the stream has been closed
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
     * Records the current read position so that a future {@link #reset()} can return to it.
     *
     * @param readLimit unused; any amount of data may be read before reset
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
     * Repositions the read cursor to the location recorded by the most recent {@link #mark(int)} call.
     *
     * @throws IOException if the stream has been closed
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
     * Moves the read position back to the very beginning of the stream without clearing data.
     *
     * @throws IOException if the stream has been closed
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
     * Releases the mark bookmark and closes the parent {@link ByteArrayIOStream}.
     */
    @Override
    public void close () {

      synchronized (ByteArrayIOStream.this) {
        markBookmark = null;

        ByteArrayIOStream.this.close();
      }
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

  /**
   * Output view into the parent {@link ByteArrayIOStream}, supporting random write positioning and rewind.
   */
  public class ByteArrayOutputStream extends OutputStream {

    /**
     * Returns the current write position in the stream.
     *
     * @return absolute byte offset of the next write
     * @throws IOException if the stream has been closed
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
     * Moves the write position to the given absolute offset, clamping to the logical end of the stream.
     *
     * @param position desired absolute byte offset
     * @throws IOException              if the stream has been closed
     * @throws IllegalArgumentException if {@code position} is negative
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
     * Writes the low eight bits of {@code b} at the current write position, expanding segments as needed.
     *
     * @param b the byte value to write
     * @throws IOException if the stream has been closed
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
     * Writes {@code len} bytes from {@code bytes} starting at offset {@code off}.
     *
     * @param bytes source data array
     * @param off   starting index in {@code bytes}
     * @param len   number of bytes to write
     * @throws IOException if the stream is closed or the offset/length are out of bounds
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

          for (int index = off; index < off + len; index++) {
            write(bytes[index]);
          }
        }
      }
    }

    /**
     * Moves the write position back to the very beginning of the stream without discarding existing data.
     *
     * @throws IOException if the stream has been closed
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
     * Moves the write position forward to the logical end of stream, ready to append new data.
     *
     * @throws IOException if the stream has been closed
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
     * Closes the parent {@link ByteArrayIOStream}.
     */
    @Override
    public void close () {

      ByteArrayIOStream.this.close();
    }
  }
}
