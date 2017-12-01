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

public class CircularBuffer {

  private static enum State {

    READ, WRITE
  }

  private RangeSegment[] segments;
  private boolean closed = false;
  private int position = 0;
  private int filled = 0;
  private byte[] buffer;

  public CircularBuffer (int size) {

    buffer = new byte[size];
    segments = new RangeSegment[2];
  }

  public synchronized boolean isClosed () {

    return closed;
  }

  public synchronized void close () {

    closed = true;
    notifyAll();
  }

  public synchronized int available () {

    return (buffer.length - filled);
  }

  public int read (byte[] data) {

    return read(data, 0);
  }

  public synchronized int read (byte[] data, long millis) {

    int bytesRead;

    if (data.length == 0) {
      return 0;
    }

    while ((bytesRead = get(data)) == 0) {
      if (closed) {
        return -1;
      }

      try {
        wait(millis);
      } catch (InterruptedException interruptedException) {
        throw new RuntimeException(interruptedException);
      }
    }

    notifyAll();
    return bytesRead;
  }

  public void write (byte[] data) {

    write(data, 0, data.length);
  }

  public synchronized void write (byte[] data, int off, int length) {

    int totalBytes = off;
    int bytesWritten;

    if (closed) {
      throw new IllegalStateException("The close() method has previously been called");
    }

    do {
      if ((bytesWritten = put(data, totalBytes, length - totalBytes)) > 0) {
        totalBytes += bytesWritten;
        notifyAll();
      }

      if (totalBytes < length) {
        try {
          wait();
        } catch (InterruptedException interruptedException) {
          throw new RuntimeException(interruptedException);
        }
      }
    } while (totalBytes < length);
  }

  private int put (byte[] data, int off, int length) {

    int totalBytes = 0;
    int writeBytes;

    setSegments(State.WRITE);
    for (int count = 0; count < segments.length; count++) {
      if (segments[count] != null) {
        writeBytes = Math.min(segments[count].getStop() - segments[count].getStart(), length - totalBytes);
        if (writeBytes > 0) {
          System.arraycopy(data, off + totalBytes, buffer, segments[count].getStart(), writeBytes);
          totalBytes += writeBytes;
        }
      }
    }

    filled += totalBytes;
    return totalBytes;
  }

  private int get (byte[] data) {

    int totalBytes = 0;
    int readBytes;

    setSegments(State.READ);
    for (int count = 0; count < segments.length; count++) {
      if (segments[count] != null) {
        readBytes = Math.min(segments[count].getStop() - segments[count].getStart(), data.length - totalBytes);
        if (readBytes > 0) {
          System.arraycopy(buffer, segments[count].getStart(), data, totalBytes, readBytes);
          totalBytes += readBytes;
        }
      }
    }

    filled -= totalBytes;
    position += totalBytes;
    if (position > buffer.length) {
      position -= buffer.length;
    }

    return totalBytes;
  }

  private void setSegments (State state) {

    if (position + filled <= buffer.length) {
      switch (state) {
        case READ:
          segments[0] = new RangeSegment(position, position + filled);
          segments[1] = null;
          break;
        case WRITE:
          segments = new RangeSegment[2];
          segments[0] = new RangeSegment(position + filled, buffer.length);
          segments[1] = new RangeSegment(0, position);
      }
    } else {
      switch (state) {
        case READ:
          segments = new RangeSegment[2];
          segments[0] = new RangeSegment(position, buffer.length);
          segments[1] = new RangeSegment(0, position + filled - buffer.length);
          break;
        case WRITE:
          segments[0] = new RangeSegment(position + filled - buffer.length, position);
          segments[1] = null;
      }
    }
  }

  public class RangeSegment {

    private int start;
    private int stop;

    public RangeSegment (int start, int stop) {

      this.start = start;
      this.stop = stop;
    }

    public int getStart () {

      return start;
    }

    public int getStop () {

      return stop;
    }
  }
}
