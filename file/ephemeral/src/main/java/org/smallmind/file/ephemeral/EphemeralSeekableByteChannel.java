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
package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import org.smallmind.nutsnbolts.io.ByteArrayIOStream;

public class EphemeralSeekableByteChannel implements SeekableByteChannel {

  private final ByteArrayIOStream stream;
  private final boolean read;
  private boolean append;
  private boolean deleteOnClose;

  @Override
  public int read (ByteBuffer dst)
    throws IOException {

    if (!read) {
      throw new NonReadableChannelException();
    } else {
      synchronized (stream) {
        if (stream.isClosed()) {
          throw new ClosedChannelException();
        } else {

          byte[] buffer = new byte[dst.remaining()];
          int bytesRead = stream.asInputStream().read(buffer);

          dst.put(buffer, 0, bytesRead);

          return bytesRead;
        }
      }
    }
  }

  @Override
  public int write (ByteBuffer src)
    throws IOException {

    if (read) {
      throw new NonWritableChannelException();
    } else {
      synchronized (stream) {
        if (stream.isClosed()) {
          throw new ClosedChannelException();
        } else {

          byte[] buffer;
          int bytesWritten = src.remaining();

          if (bytesWritten > 0) {

            buffer = new byte[bytesWritten];
            src.get(buffer);
            stream.asOutputStream().write(buffer);
          }
          return bytesWritten;
        }
      }
    }
  }

  @Override
  public long position ()
    throws IOException {

    synchronized (stream) {

      return (read) ? stream.asInputStream().position() : stream.asOutputStream().position();
    }
  }

  @Override
  public SeekableByteChannel position (long newPosition)
    throws IOException {

    if (read) {
      stream.asInputStream().position(newPosition);
    } else {
      stream.asOutputStream().position(newPosition);
    }

    return this;
  }

  @Override
  public long size ()
    throws IOException {

    return stream.size();
  }

  @Override
  public SeekableByteChannel truncate (long size) {

    stream.truncate(size);

    return this;
  }

  @Override
  public boolean isOpen () {

    return !stream.isClosed();
  }

  @Override
  public void close () {

    stream.close();
  }
}
