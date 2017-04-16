/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
import java.util.LinkedList;

public class ByteArrayIOStream implements Closeable {

  private final ByteArrayInputStream inputStream = new ByteArrayInputStream();
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final LinkedList<byte[]> segmentList = new LinkedList<>();
  private final int allocation;
  private boolean closed = false;
  private int readIndex = 0;
  private int writeIndex;

  public ByteArrayIOStream () {

    this(1024);
  }

  public ByteArrayIOStream (int allocation) {

    this.allocation = allocation;

    writeIndex = allocation;
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

    private LinkedList<byte[]> markList;
    private int readLimit = 0;
    private int markIndex = 0;

    public byte peek (int index)
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (index < 0 || index >= available()) {
          throw new IndexOutOfBoundsException(index + ">=" + available());
        } else {

          int bytesToSeek = index;

          if (bytesToSeek < allocation - readIndex) {

            return segmentList.getFirst()[readIndex + bytesToSeek];
          } else {

            int segmentIndex = 1;

            bytesToSeek -= allocation - readIndex;

            while (bytesToSeek >= allocation) {
              bytesToSeek = allocation;
              segmentIndex++;
            }

            return segmentList.get(segmentIndex)[bytesToSeek];
          }
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
        } else {

          byte singleByte;

          try {
            while (available() == 0) {
              segmentList.wait();
            }
          } catch (InterruptedException interruptedException) {
            throw new IOException(interruptedException);
          }

          singleByte = segmentList.getFirst()[readIndex];
          peelSegment(1);

          return singleByte;
        }
      }
    }

    @Override
    public int read (byte[] b, int off, int len)
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (b == null) {
          throw new NullPointerException();
        } else if (off < 0 || len < 0 || off > b.length || len > b.length - off) {
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
          int bytesRead = 0;

          while (bytesRead < bytesToRead) {

            int bytesToReadInSegment = Math.min(allocation - readIndex, bytesToRead - bytesRead);

            System.arraycopy(segmentList.getFirst(), readIndex, b, off + bytesRead, bytesToReadInSegment);
            bytesRead += bytesToReadInSegment;
            peelSegment(bytesToReadInSegment);
          }

          return bytesRead;
        }
      }
    }

    @Override
    public long skip (long n)
      throws IOException {

      long bytesToSkip;

      if ((bytesToSkip = Math.min(available(), n)) > 0) {

        long bytesSkipped = 0;

        while (bytesSkipped < bytesToSkip) {

          int bytesToSkipInSegment = (int)Math.min(allocation - readIndex, bytesToSkip - bytesSkipped);

          bytesSkipped += bytesToSkipInSegment;
          peelSegment(bytesToSkipInSegment);
        }

        return bytesSkipped;
      }

      return 0;
    }

    private void peelSegment (int readIncrement) {

      if ((readIndex += readIncrement) == allocation) {

        byte[] usedSegment = segmentList.removeFirst();

        readIndex = 0;

        if (markList != null) {
          markList.add(usedSegment);
          if (remembered() > readLimit) {
            markList = null;
          }
        }
      }
    }

    @Override
    public int available ()
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        }

        return segmentList.isEmpty() ? 0 : (allocation * (segmentList.size() - 1)) + writeIndex - readIndex;
      }
    }

    private int remembered () {

      return (markList == null) ? 0 : (allocation * markList.size()) + readIndex - markIndex;
    }

    @Override
    public void mark (int readLimit) {

      synchronized (segmentList) {
        if (!closed) {

          this.readLimit = readLimit;

          markList = new LinkedList<>();
          markIndex = readIndex;
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

        segmentList.addAll(0, markList);
        readIndex = markIndex;

        markList = null;
      }
    }

    @Override
    public void close () {

      synchronized (segmentList) {
        markList = null;

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
    public void write (byte[] b, int off, int len)
      throws IOException {

      synchronized (segmentList) {
        if (closed) {
          throw new IOException("This stream has already been closed");
        } else if (b == null) {
          throw new NullPointerException();
        } else if (off < 0 || len < 0 || off > b.length || len > b.length - off) {
          throw new IndexOutOfBoundsException();
        } else if (len > 0) {

          int bytesWritten = 0;

          while (bytesWritten < len) {

            int bytesToWriteInSegment = Math.min(allocation - writeIndex, len - bytesWritten);

            if (bytesToWriteInSegment > 0) {
              System.arraycopy(b, off + bytesWritten, segmentList.getLast(), writeIndex, bytesToWriteInSegment);
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
}
