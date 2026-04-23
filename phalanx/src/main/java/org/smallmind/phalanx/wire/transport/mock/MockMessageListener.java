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
package org.smallmind.phalanx.wire.transport.mock;

/**
 * Listener interface used by the in-memory mock transport to selectively receive and process
 * messages.  A listener first declares its interest via {@link #match} and then handles any
 * accepted message via {@link #handle}.
 */
public interface MockMessageListener {

  /**
   * Determines whether this listener should receive the message described by {@code properties}.
   *
   * @param properties metadata of the candidate message (headers, IDs, timestamps, etc.)
   * @return {@code true} if this listener wants to receive and handle the message;
   * {@code false} to skip it
   */
  boolean match (MockMessageProperties properties);

  /**
   * Processes a message that has been accepted by {@link #match}.
   *
   * @param message the accepted message to handle
   */
  void handle (MockMessage message);
}
