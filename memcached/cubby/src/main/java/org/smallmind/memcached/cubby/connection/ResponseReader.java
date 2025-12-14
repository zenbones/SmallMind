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
package org.smallmind.memcached.cubby.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.smallmind.memcached.cubby.ServerClosedException;
import org.smallmind.memcached.cubby.response.JoinedBuffer;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseParser;

/**
 * Reads and parses responses from the memcached socket channel.
 */
public class ResponseReader {

  private final SocketChannel socketChannel;
  private final ByteBuffer readBuffer;
  private final ExposedByteArrayOutputStream accumulatingStream;
  private JoinedBuffer joinedBuffer;
  private Response partialResponse;

  /**
   * Creates a reader for the provided channel.
   *
   * @param socketChannel channel to read from
   */
  public ResponseReader (SocketChannel socketChannel) {

    this.socketChannel = socketChannel;

    readBuffer = ByteBuffer.allocate(8192);
    accumulatingStream = new ExposedByteArrayOutputStream(1024);
  }

  /**
   * Attempts to read from the channel into the internal buffer.
   *
   * @return {@code true} if data was read and parsing should be attempted
   * @throws IOException if the socket is closed or an I/O error occurs
   */
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

  /**
   * Shifts remaining unread bytes to the start of the accumulation buffer.
   *
   * <p>Clears buffers when fully consumed; otherwise preserves unread data for the next read cycle.</p>
   *
   * @throws IOException if writing to the accumulation stream fails
   */
  private void shiftRemaining ()
    throws IOException {

    if (readBuffer.remaining() == 0) {
      readBuffer.clear();
      accumulatingStream.reset();
    } else {
      if (readBuffer.position() > 0) {
        accumulatingStream.reset();
      }

      byte[] remaining = new byte[readBuffer.limit() - readBuffer.position()];

      readBuffer.get(remaining);
      readBuffer.clear();

      accumulatingStream.write(remaining);
    }
  }

  /**
   * Parses a complete response from the buffered data if available.
   *
   * @return parsed response or {@code null} when incomplete
   * @throws IOException if parsing fails or I/O errors occur
   */
  public Response extract ()
    throws IOException {

    int lineLength;

    if ((lineLength = findLineEnd(joinedBuffer)) < 0) {
      shiftRemaining();

      return null;
    }
    if (partialResponse != null) {
      if (joinedBuffer.remaining() < (partialResponse.getValueLength() + 2)) {
        shiftRemaining();

        return null;
      } else {

        Response response = partialResponse;

        byte[] value = new byte[response.getValueLength()];

        joinedBuffer.get(value);
        response.setValue(value);

        joinedBuffer.incPosition(2);
        partialResponse = null;

        return response;
      }
    } else {

      Response response = ResponseParser.parse(joinedBuffer, joinedBuffer.position(), lineLength - 2);

      joinedBuffer.incPosition(2);
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

          joinedBuffer.incPosition(2);
        }
      }

      return response;
    }
  }

  /**
   * Locates the CRLF terminating the current response line.
   *
   * @param joinedBuffer buffer to scan
   * @return number of bytes up to and including the line terminator, or -1 if not found
   */
  private int findLineEnd (JoinedBuffer joinedBuffer) {

    boolean completed = false;
    int index = 0;

    while (joinedBuffer.position() + index < joinedBuffer.limit()) {
      switch (joinedBuffer.peek(index++)) {
        case '\r':
          completed = true;
          break;
        case '\n':
          if (completed) {

            return index;
          }
          break;
        default:
          completed = false;
      }
    }

    return -1;
  }
}
