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
package org.smallmind.memcached.cubby.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.smallmind.memcached.cubby.ServerClosedException;
import org.smallmind.memcached.cubby.response.JoinedBuffer;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseParser;

public class ResponseReader {

  private final SocketChannel socketChannel;
  private final ByteBuffer readBuffer;
  private final ByteArrayOutputStream accumulatingStream;
  private JoinedBuffer joinedBuffer;
  private Response partialResponse;

  public ResponseReader (SocketChannel socketChannel) {

    this.socketChannel = socketChannel;

    readBuffer = ByteBuffer.allocate(8192);
    accumulatingStream = new ByteArrayOutputStream(1024);
  }

  public boolean read ()
    throws IOException {

    int bytesRead;

    if ((bytesRead = socketChannel.read(readBuffer)) < 0) {
      throw new ServerClosedException();
    } else if (bytesRead > 0) {
      readBuffer.flip();
      joinedBuffer = new JoinedBuffer(accumulatingStream, readBuffer);

      return true;
    } else {

      return false;
    }
  }

  private void shiftRemaining ()
    throws IOException {

    if (readBuffer.remaining() == 0) {
      readBuffer.clear();
    } else {

      byte[] remaining = new byte[readBuffer.limit() - readBuffer.position()];

      readBuffer.get(remaining);
      readBuffer.clear();

      accumulatingStream.write(remaining);
    }
  }

  public Response extract ()
    throws IOException {

    int endOfLine;

    if ((endOfLine = findLineEnd(joinedBuffer)) < 0) {
      shiftRemaining();

      return null;
    } else {

      Response response = ResponseParser.parse(joinedBuffer, joinedBuffer.position(), endOfLine - 2 - joinedBuffer.position());

      joinedBuffer.position(joinedBuffer.position() + 2);
      if (response.getValueLength() >= 0) {
        if (joinedBuffer.remaining() < (response.getValueLength() + 2)) {
          partialResponse = response;
          shiftRemaining();

          return null;
        } else {
          if (response.getValueLength() > 0) {

            byte[] value = new byte[response.getValueLength()];

            joinedBuffer.get(value);
            response.setValue(value);
          }

          joinedBuffer.position(joinedBuffer.position() + 2);
        }
      }

      return response;
    }
  }

  private int findLineEnd (JoinedBuffer joinedBuffer) {

    boolean completed = false;

    joinedBuffer.mark();

    try {
      while (joinedBuffer.remaining() > 0) {
        switch (joinedBuffer.get()) {
          case '\r':
            completed = true;
            break;
          case '\n':
            if (completed) {

              return joinedBuffer.position();
            }
            break;
          default:
            completed = false;
        }
      }

      return -1;
    } finally {
      joinedBuffer.reset();
    }
  }
}
