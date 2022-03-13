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
package org.smallmind.memcached.cubby.response;

import java.nio.ByteBuffer;
import org.smallmind.memcached.cubby.connection.ExposedByteArrayOutputStream;

public class JoinedBuffer {

  private final ByteBuffer readBuffer;
  private final ByteBuffer accumulatingBuffer;
  private int position = 0;
  private int mark = -1;

  public JoinedBuffer (ExposedByteArrayOutputStream accumulatingStream, ByteBuffer readBuffer) {

    this.readBuffer = readBuffer;

    accumulatingBuffer = ByteBuffer.wrap(accumulatingStream.getBuffer(), 0, accumulatingStream.size());
  }

  public byte get () {

    if (position++ < accumulatingBuffer.limit()) {
      return accumulatingBuffer.get();
    } else {
      return readBuffer.get();
    }
  }

  public void get (byte[] buffer) {

    int bytesRead = 0;

    if (position < accumulatingBuffer.limit()) {
      accumulatingBuffer.get(buffer, 0, bytesRead = Math.min(accumulatingBuffer.remaining(), buffer.length));
    }
    if (bytesRead < buffer.length) {
      readBuffer.get(buffer, bytesRead, buffer.length - bytesRead);
    }

    position += buffer.length;
  }

  public int position () {

    return position;
  }

  public void position (int position) {

    if (position < 0) {
      throw new IllegalArgumentException("Attempt to set position < 0");
    } else if (position > accumulatingBuffer.limit() + readBuffer.limit()) {

      throw new IllegalArgumentException("Attempt to set position > " + accumulatingBuffer.limit() + readBuffer.limit());
    } else {

      this.position = position;

      if (position < accumulatingBuffer.limit()) {
        accumulatingBuffer.position(position);
        readBuffer.position(0);
      } else {
        accumulatingBuffer.position(accumulatingBuffer.limit());
        readBuffer.position(position - accumulatingBuffer.limit());
      }

      if (mark > position) {
        mark = -1;
      }
    }
  }

  public int remaining () {

    return accumulatingBuffer.limit() + readBuffer.limit() - position;
  }

  public void mark () {

    if (position < accumulatingBuffer.limit()) {
      accumulatingBuffer.mark();
    } else {
      readBuffer.mark();
    }

    mark = position;
  }

  public void reset () {

    if (mark >= 0) {
      if (mark < accumulatingBuffer.limit()) {
        accumulatingBuffer.reset();
        readBuffer.position(0);
      } else {
        accumulatingBuffer.position(accumulatingBuffer.limit());
        readBuffer.reset();
      }

      position = mark;
    }
  }
}
