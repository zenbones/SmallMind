/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class ByteArrayIOStream implements Closeable {

  private final ByteArrayInputStream inputStream = new ByteArrayInputStream();
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final ByteArrayIOBuffer segmentBuffer;
  private final ByteArrayIOBookmark readBookmark;
  private final ByteArrayIOBookmark writeBookmark;
  private boolean closed = false;

  public ByteArrayIOStream () {

    this(1024);
  }

  public ByteArrayIOStream (int allocation) {

    segmentBuffer = new ByteArrayIOBuffer(allocation);
    readBookmark = new ByteArrayIOBookmark(allocation);
    writeBookmark = new ByteArrayIOBookmark(allocation);
  }

  public ByteArrayIOStream (ByteArrayIOBuffer segmentBuffer) {

    this.segmentBuffer = segmentBuffer;

    readBookmark = new ByteArrayIOBookmark(segmentBuffer.getAllocation());
    writeBookmark = new ByteArrayIOBookmark(segmentBuffer.getAllocation());
  }

  public synchronized boolean isClosed () {

    return closed;
  }

  @Override
  public synchronized void close () {

    closed = true;
  }

  public synchronized long size ()
    throws IOException {

    if (closed) {
      throw new IOException("This stream has already been closed");
    } else {

      return segmentBuffer.getLimitBookmark().position();
    }
  }

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

  public ByteArrayInputStream asInputStream () {

    return inputStream;
  }

  public ByteArrayOutputStream asOutputStream () {

    return outputStream;
  }

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

    @Override
    public void mark (int readLimit) {

      synchronized (ByteArrayIOStream.this) {
        if (!closed) {
          markBookmark = new ByteArrayIOBookmark(readBookmark);
        }
      }
    }

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

    public void rewind ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        readBookmark.rewind();
      }
    }

    @Override
    public void close () {

      synchronized (ByteArrayIOStream.this) {
        markBookmark = null;

        ByteArrayIOStream.this.close();
      }
    }

    @Override
    public boolean markSupported () {

      return true;
    }
  }

  public class ByteArrayOutputStream extends OutputStream {

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

    public void rewind ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        writeBookmark.rewind();
      }
    }

    public void advance ()
      throws IOException {

      synchronized (ByteArrayIOStream.this) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        writeBookmark.reset(segmentBuffer.getLimitBookmark());
      }
    }

    @Override
    public void close () {

      ByteArrayIOStream.this.close();
    }
  }
}
