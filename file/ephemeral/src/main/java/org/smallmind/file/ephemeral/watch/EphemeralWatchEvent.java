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
package org.smallmind.file.ephemeral.watch;

import java.nio.file.WatchEvent;

/**
 * Simple watch event implementation used by the ephemeral watch service.
 *
 * @param <T> the context type supplied with the event
 */
public class EphemeralWatchEvent<T> implements WatchEvent<T> {

  private final Kind<T> kind;
  private final T context;
  private final int count;

  /**
   * Creates a new event.
   *
   * @param kind    the event kind that occurred
   * @param count   the number of times the event was coalesced
   * @param context the context for the event, such as the affected path segment
   */
  public EphemeralWatchEvent (Kind<T> kind, int count, T context) {

    this.kind = kind;
    this.count = count;
    this.context = context;
  }

  /**
   * @return the kind of watch event
   */
  @Override
  public Kind<T> kind () {

    return kind;
  }

  /**
   * @return the number of occurrences that have been coalesced
   */
  @Override
  public int count () {

    return count;
  }

  /**
   * @return the event context
   */
  @Override
  public T context () {

    return context;
  }
}
