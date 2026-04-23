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
 * Couples routing intent with conversational context for a single outbound wire request.
 *
 * <p>A {@code Voice} pairs a {@link VocalMode} — controlling whether the request is directed
 * at a specific service instance or broadcast to an entire group — with a {@link Conversation}
 * that governs the exchange pattern and timeout. Transport components consume a {@code Voice}
 * when dispatching calls.</p>
 *
 * @param <G> the type used to identify the target service group
 * @param <I> the type used to identify a specific service instance
 */
public interface Voice<G, I> {

  /**
   * Returns the routing mode for this voice.
   *
   * @return the {@link VocalMode} indicating directed or broadcast delivery
   */
  VocalMode getMode ();

  /**
   * Returns the conversation governing the exchange pattern and timeout for this voice.
   *
   * @return the associated {@link Conversation}; never {@code null}
   */
  Conversation<?> getConversation ();

  /**
   * Returns the logical identifier of the target service group.
   *
   * @return the service group identifier; never {@code null}
   */
  G getServiceGroup ();

  /**
   * Returns the identifier of the specific service instance to target, if any.
   *
   * @return the instance identifier, or {@code null} when the voice targets all instances
   * in the service group
   */
  I getInstanceId ();
}
