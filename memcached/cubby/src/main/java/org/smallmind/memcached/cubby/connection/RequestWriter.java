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
 * Accumulates serialized memcached commands into a {@link ByteBuffer} and writes them to
 * the underlying {@link SocketChannel} as space allows.
 *
 * <p>Because NIO writes may not consume an entire buffer in one call, this class maintains
 * a <em>draining</em> state: once a write call does not exhaust all buffered data the writer
 * enters draining mode and refuses new commands until the backlog is cleared. Similarly, if
 * a command's byte array is too large to fit entirely in the buffer in one shot the overflow
 * portion is tracked in {@code unfinishedCommandBuffer} and prepended on the next
 * {@link #prepare()} call.</p>
 *
 * <p>The typical call sequence from the NIO selector loop is:
 * <ol>
 *   <li>{@link #prepare()} — returns {@code true} when the buffer is ready for new commands.</li>
 *   <li>{@link #add(CommandBuffer)} — copies bytes into the buffer; returns {@code false} when
 *       the buffer is full and no more commands should be added this cycle.</li>
 *   <li>{@link #write()} — flips and drains the buffer to the channel.</li>
 * </ol>
 * </p>
 */
public class RequestWriter {

  private final SocketChannel socketChannel;
  private final ByteBuffer writeBuffer;
  private CommandBuffer unfinishedCommandBuffer;
  private boolean draining = false;
  private int unfinishedCommandIndex = 0;

  /**
   * Creates a writer that targets the given channel and sizes its internal write buffer to
   * match the channel socket's configured send buffer. Falls back to 8192 bytes if the
   * socket option is not available.
   *
   * @param socketChannel the non-blocking channel to write commands to
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
   * Prepares the write buffer for a new batch of commands.
   *
   * <p>If the writer is currently draining a previous partial write it returns {@code false}
   * immediately. Otherwise, if there is a partially written command from the previous cycle it
   * attempts to copy more of that command's bytes into the buffer. Returns {@code true} only
   * when the buffer is clear of any leftover data and ready to accept new commands via
   * {@link #add(CommandBuffer)}.</p>
   *
   * @return {@code true} if the buffer is ready to accept additional commands; {@code false}
   * if it is still draining or still processing a command that did not fit last cycle
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
   * Attempts to copy the bytes from the given {@link CommandBuffer} into the write buffer.
   *
   * <p>If the entire command fits in the remaining buffer space the method returns {@code true}
   * and the caller may attempt to add another command. If the command is too large or the buffer
   * is full, the command (or its unwritten portion) is stored as the unfinished command for the
   * next prepare cycle and {@code false} is returned, signalling that no more commands should be
   * added this write cycle.</p>
   *
   * @param commandBuffer the serialized command to enqueue into the write buffer
   * @return {@code true} if the entire command was copied into the buffer and additional commands
   * may still be added; {@code false} if the buffer is full and a subsequent
   * {@link #write()} call is needed before more commands can be accommodated
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
   * Writes the contents of the internal buffer to the socket channel.
   *
   * <p>On the first call after one or more {@link #add(CommandBuffer)} invocations the buffer
   * is flipped before writing. If the channel does not accept all bytes in a single call
   * (partial write), the writer enters draining mode and subsequent calls will continue
   * flushing the remaining data without accepting new commands. Once all bytes are written the
   * buffer is cleared and draining mode is exited.</p>
   *
   * @throws IOException if the channel encounters an error during the write operation
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
