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
import java.util.ArrayList;

public class ByteArrayIOStream implements Closeable {

  private final ByteArrayInputStream inputStream = new ByteArrayInputStream();
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final ArrayList<byte[]> segmentList = new ArrayList<>();
  private final Bookmark limitBookmark = new Bookmark();
  private final Bookmark readBookmark = new Bookmark();
  private final Bookmark writeBookmark = new Bookmark();
  private final long allocation;
  private boolean closed = false;

  public ByteArrayIOStream () {

    this(1024);
  }

  public ByteArrayIOStream (long allocation) {

    this.allocation = allocation;
  }

  @Override
  public void close () {

    synchronized (segmentList) {
      closed = true;
      segmentList.clear();
    }
  }

  public ByteArrayInputStream asInputStream () {

    return inputStream;
  }

  public ByteArrayOutputStream asOutputStream () {

    return outputStream;
  }

  public class ByteArrayInputStream extends InputStream {

    private Bookmark markBookmark;

    public byte peek (int index)
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (index < 0 || index >= available()) {
          throw new IndexOutOfBoundsException(index + ">=" + available());
        } else {

          Bookmark peekBookmark = readBookmark.offset(index);

          return segmentList.get(peekBookmark.segmentIndex())[peekBookmark.byteIndex()];
        }
      }
    }

    public byte[] readAvailable ()
      throws IOException {

      synchronized (segmentList) {

        byte[] buffer = new byte[available()];

        read(buffer);

        return buffer;
      }
    }

    @Override
    public int read ()
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if ((readBookmark.segmentIndex() == limitBookmark.segmentIndex()) && (readBookmark.byteIndex() > limitBookmark.byteIndex())) {

          return -1;
        } else {

          byte singleByte;

          try {
            while (available() == 0) {
              segmentList.wait();
            }
          } catch (InterruptedException interruptedException) {
            throw new IOException(interruptedException);
          }

          singleByte = segmentList.get(readBookmark.segmentIndex())[readBookmark.byteIndex()];
          readBookmark.inc();

          return singleByte;
        }
      }
    }

    @Override
    public int read (byte[] bytes, int off, int len)
      throws IOException {

      synchronized (segmentList) {
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

          try {
            while ((bytesAvailable = available()) == 0) {
              segmentList.wait();
            }
          } catch (InterruptedException interruptedException) {
            throw new IOException(interruptedException);
          }

          int bytesToRead = Math.min(bytesAvailable, len);

          for (int index = 0; index < bytesToRead; index++) {
            bytes[off + index] = segmentList.get(readBookmark.segmentIndex())[readBookmark.byteIndex()];
            readBookmark.inc();
          }

          return bytesToRead;
        }
      }
    }

    @Override
    public long skip (long n)
      throws IOException {

      long bytesToSkip;

      if ((bytesToSkip = Math.min(available(), n)) > 0) {

        readBookmark.skip(bytesToSkip);

        return bytesToSkip;
      }

      return 0;
    }

    @Override
    public int available ()
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        return (int)(limitBookmark.position() - readBookmark.position());
      }
    }

    @Override
    public void mark (int readLimit) {

      synchronized (segmentList) {
        if (!closed) {
          markBookmark = new Bookmark(readBookmark);
        }
      }
    }

    @Override
    public void reset ()
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        readBookmark.reset(markBookmark);
      }
    }

    @Override
    public void close () {

      synchronized (segmentList) {
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

    @Override
    public void write (int b)
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        if (writeIndex == allocation) {
          segmentList.add(new byte[allocation]);
          writeIndex = 0;
        }

        segmentList.getLast()[writeIndex++] = (byte)b;
        segmentList.notify();
      }
    }

    @Override
    public void write (byte[] bytes, int off, int len)
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (bytes == null) {
          throw new NullPointerException();
        } else if (off < 0 || len < 0 || off > bytes.length || len > bytes.length - off) {
          throw new IndexOutOfBoundsException();
        } else if (len > 0) {

          int bytesWritten = 0;

          while (bytesWritten < len) {

            int bytesToWriteInSegment = Math.min(allocation - writeIndex, len - bytesWritten);

            if (bytesToWriteInSegment > 0) {
              System.arraycopy(bytes, off + bytesWritten, segmentList.getLast(), writeIndex, bytesToWriteInSegment);
              bytesWritten += bytesToWriteInSegment;
            }
            if ((writeIndex += bytesToWriteInSegment) == allocation) {
              segmentList.add(new byte[allocation]);
              writeIndex = 0;
            }
          }
        }
      }
    }

    @Override
    public void close () {

      ByteArrayIOStream.this.close();
    }
  }

  private class Bookmark {

    private int segmentIndex;
    private int byteIndex;

    public Bookmark () {

    }

    public Bookmark (long position) {

      segmentIndex = (int)(position / allocation);
      byteIndex = (int)(position % allocation);
    }

    public Bookmark (Bookmark bookmark) {

      segmentIndex = bookmark.segmentIndex();
      byteIndex = bookmark.byteIndex();
    }

    public int segmentIndex () {

      return segmentIndex;
    }

    public int byteIndex () {

      return byteIndex;
    }

    public long position () {

      return (segmentIndex * allocation) + byteIndex;
    }

    public Bookmark reset (Bookmark bookmark) {

      if (bookmark != null) {
        segmentIndex = bookmark.segmentIndex();
        byteIndex = bookmark.byteIndex();
      }

      return this;
    }

    public Bookmark inc () {

      if (segmentIndex > limitBookmark.segmentIndex()) {
        throw new IllegalStateException("End of stream");
      } else if (segmentIndex < limitBookmark.segmentIndex()) {
        if (++byteIndex == allocation) {
          byteIndex = 0;
          segmentIndex++;
        }
      } else if (byteIndex <= limitBookmark.byteIndex()) {
        byteIndex++;
      } else {
        throw new IllegalStateException("End of stream");
      }

      return this;
    }

    public Bookmark skip (long n) {

      long futurePosition = position() + n;

      if (futurePosition > limitBookmark.position()) {
        throw new IllegalArgumentException("Offset not within bounds");
      } else {

        segmentIndex = (int)(futurePosition / allocation);
        byteIndex = (int)(futurePosition % allocation);

        return this;
      }
    }

    public Bookmark offset (long delta) {

      long futurePosition = position() + delta;

      if ((futurePosition < 0) || (futurePosition > limitBookmark.position())) {
        throw new IllegalArgumentException("Offset not within bounds");
      } else {

        return new Bookmark(futurePosition);
      }
    }
  }
}
