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
package org.smallmind.file.jailed;

import java.nio.file.WatchEvent;

/**
 * A {@link WatchEvent} that carries a context translated into jail space.
 *
 * <p>Instances are produced by {@link JailedWatchKey#pollEvents()} in place of the native events
 * they mirror, so that the context of a directory-entry event is a {@link JailedPath} rather
 * than a path on the backing native file system.
 *
 * @param <T> the type of the context object carried by this event
 * @see JailedWatchKey
 */
public class JailedWatchEvent<T> implements WatchEvent<T> {

  /**
   * The kind of event, mirrored from the native event.
   */
  private final Kind<T> kind;

  /**
   * The context of the event, translated into jail space.
   */
  private final T context;

  /**
   * The number of times the event was observed, mirrored from the native event.
   */
  private final int count;

  /**
   * Constructs an event mirroring a native watch event.
   *
   * @param kind    the kind of the native event
   * @param count   the repeat count of the native event
   * @param context the context of the native event, already translated into jail space
   */
  public JailedWatchEvent (Kind<T> kind, int count, T context) {

    this.kind = kind;
    this.count = count;
    this.context = context;
  }

  /**
   * Returns the kind of this event.
   *
   * @return the event kind, as reported by the native event
   */
  @Override
  public Kind<T> kind () {

    return kind;
  }

  /**
   * Returns the number of times this event was observed.
   *
   * @return the repeat count, as reported by the native event
   */
  @Override
  public int count () {

    return count;
  }

  /**
   * Returns the context of this event.
   *
   * @return the context in jail space, which for a directory-entry event is the relative
   * {@link JailedPath} of the entry within the watched directory
   */
  @Override
  public T context () {

    return context;
  }
}
