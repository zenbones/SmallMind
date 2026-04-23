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
 * An immutable value object that pairs a serialized memcached command (as a raw byte array)
 * with a monotonically increasing sequence index.
 *
 * <p>The index records the order in which the command was enqueued so that the
 * {@link RequestWriter} can preserve write ordering when draining the request queue.
 * Instances are created by {@link NIOCubbyConnection} and passed through the request and
 * response queues as part of a {@link MissingLink}.</p>
 */
public class CommandBuffer {

  private final byte[] request;
  private final long index;

  /**
   * Creates a new buffer holding the serialized form of a single command.
   *
   * @param index   monotonically increasing ordinal that establishes the write order for
   *                this command relative to others queued on the same connection
   * @param request fully serialized, wire-format bytes of the memcached command;
   *                the array is stored by reference and must not be modified by the caller
   *                after construction
   */
  public CommandBuffer (long index, byte[] request) {

    this.index = index;
    this.request = request;
  }

  /**
   * Returns the sequence index assigned when this command was enqueued.
   *
   * @return the write-order index for this command
   */
  public long getIndex () {

    return index;
  }

  /**
   * Returns the raw, wire-format bytes of the serialized memcached command.
   *
   * @return the serialized request bytes; the array must not be modified by callers
   */
  public byte[] getRequest () {

    return request;
  }
}
