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
 * Couples a serialized command with its callback while in-flight on the connection.
 */
public class MissingLink {

  private final RequestCallback requestCallback;
  private final CommandBuffer commandBuffer;

  /**
   * Constructs a link entry for the given request.
   *
   * @param requestCallback callback expecting the response
   * @param commandBuffer   serialized command buffer
   */
  public MissingLink (RequestCallback requestCallback, CommandBuffer commandBuffer) {

    this.requestCallback = requestCallback;
    this.commandBuffer = commandBuffer;
  }

  /**
   * @return callback to notify when a response arrives
   */
  public RequestCallback getRequestCallback () {

    return requestCallback;
  }

  /**
   * @return serialized command buffer
   */
  public CommandBuffer getCommandBuffer () {

    return commandBuffer;
  }
}
