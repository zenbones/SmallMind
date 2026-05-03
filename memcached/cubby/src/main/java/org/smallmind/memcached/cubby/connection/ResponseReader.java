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
 * Reads raw bytes from a memcached {@link SocketChannel} and incrementally parses them into
 * complete {@link Response} objects.
 *
 * <p>Because NIO reads may deliver partial responses, this class maintains an
 * {@link ExposedByteArrayOutputStream} as an accumulation buffer that preserves unprocessed
 * bytes between read cycles. A fixed-size {@link ByteBuffer} is used for each channel read;
 * any bytes not yet consumed by the parser are compacted back into the accumulation buffer
 * via {@link #shiftRemaining()}. This approach avoids allocating new byte arrays on every
 * read while still handling arbitrarily fragmented network delivery.</p>
 *
 * <p>The typical call pattern from the NIO selector loop is:
 * <ol>
 *   <li>{@link #read()} — fills the buffer from the channel; returns {@code true} if data
 *       was received.</li>
 *   <li>{@link #extract()} — called repeatedly until it returns {@code null}, yielding one
 *       fully parsed {@link Response} per call.</li>
 * </ol>
 */
public class ResponseReader {

  private final SocketChannel socketChannel;
  private final ByteBuffer readBuffer;
  private final ExposedByteArrayOutputStream accumulatingStream;
  private JoinedBuffer joinedBuffer;
  private Response partialResponse;

  /**
   * Creates a reader bound to the given channel, using an 8192-byte read buffer and a
   * 1024-byte initial accumulation stream.
   *
   * @param socketChannel the non-blocking channel from which server responses are read
   */
  public ResponseReader (SocketChannel socketChannel) {

    this.socketChannel = socketChannel;

    readBuffer = ByteBuffer.allocate(8192);
    accumulatingStream = new ExposedByteArrayOutputStream(1024);
  }

  /**
   * Attempts a single non-blocking read from the channel into the internal read buffer.
   *
   * <p>If the channel returns {@code -1} the server has closed the connection and a
   * {@link ServerClosedException} is thrown. If at least one byte was read the buffer is
   * flipped and a {@link JoinedBuffer} is created that presents both any accumulated leftover
   * bytes and the freshly read bytes as a single logical view, returning {@code true} to
   * signal that {@link #extract()} should be called. If no bytes are available yet
   * {@code false} is returned.</p>
   *
   * @return {@code true} if bytes were read and parsing should proceed; {@code false} if the
   * channel had no data ready
   * @throws IOException if the server closed the connection ({@link ServerClosedException})
   *                     or another I/O error occurs during the read
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
   * Moves any bytes not yet consumed by the parser into the accumulation stream for the next
   * read cycle, then resets the read buffer for the next channel read.
   *
   * <p>If all bytes in the read buffer have been consumed both buffers are simply cleared.
   * If unconsumed bytes remain they are written to the accumulation stream and the read buffer
   * is cleared so the next channel read starts at position zero.</p>
   *
   * @throws IOException if writing remaining bytes to the accumulation stream fails
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
   * Attempts to parse one complete {@link Response} from the bytes currently available in the
   * joined buffer.
   *
   * <p>The method first scans for a CRLF line terminator using {@link #findLineEnd(JoinedBuffer)}.
   * If no terminator is found the unprocessed bytes are shifted to the accumulation buffer and
   * {@code null} is returned. Once a header line is available it is parsed by
   * {@link ResponseParser}. If the response includes a value body the method additionally
   * checks that the full body (plus the trailing CRLF) is present; if not, the partial response
   * is saved and {@code null} is returned so the caller will try again after the next
   * {@link #read()}.</p>
   *
   * @return a fully parsed {@link Response}, or {@code null} if insufficient data is available
   * and the caller should wait for more bytes from the channel
   * @throws IOException if the response header cannot be parsed or an I/O error occurs while
   *                     reading from the accumulation stream
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
   * Scans the joined buffer starting at the current position for a CRLF ({@code \r\n})
   * sequence that marks the end of a memcached response header line.
   *
   * @param joinedBuffer the buffer to scan; its position is not modified by this method
   * @return the number of bytes from the current buffer position up to and including the
   * {@code \n} of the CRLF terminator, or {@code -1} if no complete CRLF sequence
   * is found within the available data
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
