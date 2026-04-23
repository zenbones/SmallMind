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
package org.smallmind.quorum.pool.complex.event;

import java.util.EventObject;
import org.smallmind.quorum.pool.complex.ComponentPool;

/**
 * Base class for all events emitted by a complex component pool.
 * <p>
 * Extends {@link EventObject} with a typed source reference so that subclasses and listeners
 * can access the originating pool without an unchecked cast. The source is recorded by
 * {@link EventObject#getSource()} as an {@link Object}, but callers that know the
 * component type can use the type parameter for safe downcasting.
 *
 * @param <C> the type of component managed by the originating pool
 */
public abstract class ComponentPoolEvent<C> extends EventObject {

  /**
   * Creates an event sourced from the given pool.
   *
   * @param componentPool the pool that generated this event; stored as the {@code source}
   *                      by {@link EventObject}
   */
  public ComponentPoolEvent (ComponentPool<C> componentPool) {

    super(componentPool);
  }
}
