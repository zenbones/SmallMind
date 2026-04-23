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
package org.smallmind.phalanx.wire;

/**
 * Defines the exchange pattern and optional timeout for a single wire request.
 *
 * <p>Implementations distinguish between fire-and-forget ({@link ConversationType#IN_ONLY})
 * and request/response ({@link ConversationType#IN_OUT}) exchanges. The timeout type parameter
 * reflects whether a deadline is meaningful: use {@link Void} for one-way conversations and
 * {@link Long} for timed two-way conversations.</p>
 *
 * @param <T> the timeout value type; {@link Void} for one-way, {@link Long} for two-way
 */
public interface Conversation<T> {

  /**
   * Returns the exchange pattern for this conversation.
   *
   * @return {@link ConversationType#IN_ONLY} for fire-and-forget calls, or
   * {@link ConversationType#IN_OUT} for request/response calls
   */
  ConversationType getConversationType ();

  /**
   * Returns the timeout associated with this conversation.
   *
   * <p>Returns {@code null} for one-way conversations. For two-way conversations, a positive
   * value overrides the service-level default; zero or a negative value defers to that default.</p>
   *
   * @return the timeout value, or {@code null} if no timeout applies
   */
  T getTimeout ();
}
