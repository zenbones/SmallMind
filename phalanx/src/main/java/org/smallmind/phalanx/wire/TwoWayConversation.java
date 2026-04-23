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
 * {@link Conversation} implementation for request/response calls that expect a reply.
 *
 * <p>Carries a timeout value in seconds. A positive value overrides the service-level default;
 * zero or a negative value defers to that default. Use the no-argument constructor when the
 * service default is acceptable.</p>
 */
public class TwoWayConversation implements Conversation<Long> {

  Long timeout;

  /**
   * Creates a two-way conversation that defers to the service-level default timeout.
   */
  public TwoWayConversation () {

    this(0L);
  }

  /**
   * Creates a two-way conversation with an explicit timeout.
   *
   * @param timeout the timeout in seconds; a positive value overrides the service default,
   *                zero or negative defers to the service default
   */
  public TwoWayConversation (Long timeout) {

    this.timeout = timeout;
  }

  /**
   * Returns {@link ConversationType#IN_OUT}, indicating that a response is expected.
   *
   * @return {@link ConversationType#IN_OUT}
   */
  @Override
  public ConversationType getConversationType () {

    return ConversationType.IN_OUT;
  }

  /**
   * Returns the timeout in seconds for this conversation.
   *
   * <p>A positive value overrides the service-level default; zero or a negative value
   * indicates that the service default applies.</p>
   *
   * @return the timeout in seconds; never {@code null}
   */
  @Override
  public Long getTimeout () {

    return timeout;
  }
}
