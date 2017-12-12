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
import java.util.function.Supplier;

public class CircularBufferIOStream implements Closeable {

  private static final int DEFAULT_SIZE = 1024;

  private final InputStream inputStream = new MarkableInputStream(new CircularBufferInputStream());
  private final OutputStream outputStream = new CircularBufferOutputStream();
  private final CircularBuffer circularBuffer;
  private final Supplier<Integer> availableSupplier;
  private boolean inputClosed = false;
  private boolean outputClosed = false;

  public CircularBufferIOStream () {

    this(DEFAULT_SIZE, null);
  }

  public CircularBufferIOStream (int size) {

    this(size, null);
  }

  public CircularBufferIOStream (Supplier<Integer> availableSupplier) {

    this(DEFAULT_SIZE, availableSupplier);
  }

  public CircularBufferIOStream (int size, Supplier<Integer> availableSupplier) {

    this.availableSupplier = availableSupplier;

    circularBuffer = new CircularBuffer(size);
  }

  @Override
  public void close () {

    circularBuffer.close();
  }

  public InputStream asInputStream () {

    return inputStream;
  }

  public OutputStream asOutputStream () {

    return outputStream;
  }

  public class CircularBufferInputStream extends InputStream {

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
    public synchronized void reset ()
      throws IOException {

      throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported () {

      return false;
    }

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
