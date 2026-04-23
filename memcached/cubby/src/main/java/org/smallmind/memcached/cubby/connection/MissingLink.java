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

/**
 * An immutable pairing that binds a serialized command ({@link CommandBuffer}) to the
 * {@link RequestCallback} that must be notified when the server's response arrives.
 *
 * <p>Instances travel through two queues inside {@link NIOCubbyConnection}: they are first
 * placed on the <em>request queue</em> where the write side of the NIO loop picks them up and
 * sends the bytes to the server, then immediately moved to the <em>response queue</em> so the
 * read side can correlate the incoming response with the waiting callback. Because memcached
 * answers requests in order, the two queues stay synchronized and no additional correlation
 * identifier is needed.</p>
 *
 * <p>The callback may be {@code null} for server-initiated maintenance commands (such as
 * keep-alive NOOPs) where no client is waiting for a result.</p>
 */
public class MissingLink {

  private final RequestCallback requestCallback;
  private final CommandBuffer commandBuffer;

  /**
   * Creates a new link coupling the given callback to its outgoing command.
   *
   * @param requestCallback callback to invoke when the matching response is received;
   *                        may be {@code null} if no caller is waiting for the result
   * @param commandBuffer   the serialized wire-format bytes of the command to send
   */
  public MissingLink (RequestCallback requestCallback, CommandBuffer commandBuffer) {

    this.requestCallback = requestCallback;
    this.commandBuffer = commandBuffer;
  }

  /**
   * Returns the callback to notify when the server's response for this command is parsed.
   *
   * @return the associated {@link RequestCallback}, or {@code null} if no caller is waiting
   */
  public RequestCallback getRequestCallback () {

    return requestCallback;
  }

  /**
   * Returns the buffered, serialized command that will be written to the socket channel.
   *
   * @return the {@link CommandBuffer} containing the raw request bytes and sequence index
   */
  public CommandBuffer getCommandBuffer () {

    return commandBuffer;
  }
}
