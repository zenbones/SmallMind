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
package org.smallmind.memcached.cubby.response;

import java.nio.ByteBuffer;
import org.smallmind.memcached.cubby.connection.ExposedByteArrayOutputStream;
import org.smallmind.nutsnbolts.lang.FormattedIllegalArgumentException;

public class JoinedBuffer {

  private static final byte[] NO_BYTES = new byte[0];

  private final ByteBuffer readBuffer;
  private final ByteBuffer accumulatingBuffer;
  private final int limit;
  private int position = 0;
  private int mark = -1;

  public JoinedBuffer (ExposedByteArrayOutputStream accumulatingStream, ByteBuffer readBuffer) {

    this.readBuffer = readBuffer;

    accumulatingBuffer = ByteBuffer.wrap(accumulatingStream.getBuffer(), 0, accumulatingStream.size());

    limit = accumulatingStream.size() + readBuffer.limit();
  }

  public byte peek (int index) {

    return get(position() + index);
  }

  public byte get () {

    if (position++ < accumulatingBuffer.limit()) {
      return accumulatingBuffer.get();
    } else {
      return readBuffer.get();
    }
  }

  public byte get (int index) {

    if (index < accumulatingBuffer.limit()) {
      return accumulatingBuffer.get(index);
    } else {
      return readBuffer.get(index - accumulatingBuffer.limit());
    }
  }

  public byte[] get (byte[] buffer) {

    int bytesRead = 0;

    if (position < accumulatingBuffer.limit()) {
      accumulatingBuffer.get(buffer, 0, bytesRead = Math.min(accumulatingBuffer.remaining(), buffer.length));
    }
    if (bytesRead < buffer.length) {
      readBuffer.get(buffer, bytesRead, buffer.length - bytesRead);
    }

    position += buffer.length;

    return buffer;
  }

  public int limit () {

    return limit;
  }

  public int position () {

    return position;
  }

  public int incPosition (int delta) {

    position(position + delta);

    return position;
  }

  public void position (int position) {

    if (position < 0) {
      throw new FormattedIllegalArgumentException("Attempt to set position < 0");
    } else if (position > accumulatingBuffer.limit() + readBuffer.limit()) {

      throw new FormattedIllegalArgumentException("Attempt to set position > %d", accumulatingBuffer.limit() + readBuffer.limit());
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

    return limit - position;
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
