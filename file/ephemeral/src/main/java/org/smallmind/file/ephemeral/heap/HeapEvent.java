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
package org.smallmind.file.ephemeral.heap;

import java.util.EventObject;
import org.smallmind.file.ephemeral.EphemeralPath;

/**
 * Describes a change that occurred within the ephemeral heap file-system tree.
 *
 * <p>A {@code HeapEvent} is created whenever a {@link HeapNode} is created, deleted, or
 * modified. It carries the affected {@link EphemeralPath} and the {@link HeapEventType}
 * that categorises the change. Events are propagated upward through the tree by
 * {@link HeapNode#bubble(HeapEvent)} and ultimately delivered to registered
 * {@link HeapEventListener} instances, which translate them into
 * {@link java.nio.file.WatchEvent}s for the NIO watch-service subsystem.
 *
 * @see HeapEventType
 * @see HeapEventListener
 * @see HeapNode#bubble(HeapEvent)
 */
public class HeapEvent extends EventObject {

  /**
   * The kind of change represented by this event.
   */
  private final HeapEventType type;

  /**
   * The path within the ephemeral file system that was affected by the change.
   */
  private final EphemeralPath path;

  /**
   * Constructs a new heap event.
   *
   * @param source the object that generated the event (typically the {@link HeapNode}
   *               on which the change occurred)
   * @param path   the {@link EphemeralPath} that was created, deleted, or modified
   * @param type   the {@link HeapEventType} that categorises the kind of change
   */
  public HeapEvent (Object source, EphemeralPath path, HeapEventType type) {

    super(source);

    this.path = path;
    this.type = type;
  }

  /**
   * Returns the kind of heap change that this event represents.
   *
   * @return the {@link HeapEventType} for this event; never {@code null}
   */
  public HeapEventType getType () {

    return type;
  }

  /**
   * Returns the ephemeral path that was affected by the change.
   *
   * @return the {@link EphemeralPath} associated with this event; never {@code null}
   */
  public EphemeralPath getPath () {

    return path;
  }
}
