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
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Buffers and writes serialized commands to the memcached socket channel.
 */
public class RequestWriter {

  private final SocketChannel socketChannel;
  private final ByteBuffer writeBuffer;
  private CommandBuffer unfinishedCommandBuffer;
  private boolean draining = false;
  private int unfinishedCommandIndex = 0;

  /**
   * Creates a writer tied to the provided channel and sized to its send buffer.
   *
   * @param socketChannel channel to write to
   */
  public RequestWriter (SocketChannel socketChannel) {

    int sendBufferSize;

    this.socketChannel = socketChannel;

    try {
      sendBufferSize = socketChannel.socket().getSendBufferSize();
    } catch (SocketException socketException) {
      sendBufferSize = 8192;
    }

    writeBuffer = ByteBuffer.allocate(sendBufferSize);
  }

  /**
   * Prepares the write buffer for accepting additional commands.
   *
   * @return {@code true} if additional commands can be added to the buffer
   */
  public boolean prepare () {

    if (draining) {

      return false;
    } else if (unfinishedCommandBuffer == null) {

      return true;
    } else if (writeBuffer.remaining() > 0) {

      byte[] request;
      int bytesRead;

      writeBuffer.put(request = unfinishedCommandBuffer.getRequest(), unfinishedCommandIndex, bytesRead = Math.min(writeBuffer.remaining(), request.length - unfinishedCommandIndex));
      unfinishedCommandIndex += bytesRead;

      if (unfinishedCommandIndex == request.length) {
        unfinishedCommandBuffer = null;
        unfinishedCommandIndex = 0;

        return true;
      } else {

        return false;
      }
    } else {

      return false;
    }
  }

  /**
   * Attempts to queue the given command into the buffer.
   *
   * @param commandBuffer serialized command data
   * @return {@code true} if the command fully fit in the buffer
   */
  public boolean add (CommandBuffer commandBuffer) {

    if ((!draining) && (writeBuffer.remaining() > 0)) {

      byte[] request;
      int bytesRead;

      writeBuffer.put(request = commandBuffer.getRequest(), 0, bytesRead = Math.min(writeBuffer.remaining(), request.length));

      if (bytesRead == request.length) {

        return true;
      } else {
        unfinishedCommandBuffer = commandBuffer;
        unfinishedCommandIndex = bytesRead;

        return false;
      }
    } else {
      unfinishedCommandBuffer = commandBuffer;
      unfinishedCommandIndex = 0;

      return false;
    }
  }

  /**
   * Writes buffered commands to the channel.
   *
   * @throws IOException if the channel fails while writing
   */
  public void write ()
    throws IOException {

    if (!draining) {
      writeBuffer.flip();
    }

    if (writeBuffer.position() < writeBuffer.limit()) {
      socketChannel.write(writeBuffer);
    }

    if (writeBuffer.position() < writeBuffer.limit()) {
      draining = true;
    } else {
      draining = false;
      writeBuffer.clear();
    }
  }
}
