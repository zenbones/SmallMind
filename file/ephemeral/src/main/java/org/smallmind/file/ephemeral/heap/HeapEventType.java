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

import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

/**
 * Enumerates the kinds of changes that can occur within the ephemeral heap file-system tree
 * and maps each kind to its corresponding NIO {@link WatchEvent.Kind}.
 *
 * <p>Each constant wraps a {@link StandardWatchEventKinds} value so that
 * {@link HeapEventListener} implementations can forward events to the NIO
 * {@link java.nio.file.WatchService} subsystem without additional mapping logic.
 *
 * @see HeapEvent
 * @see HeapEventListener
 */
public enum HeapEventType {

  /**
   * A new file or directory was created within a watched directory.
   * Maps to {@link StandardWatchEventKinds#ENTRY_CREATE}.
   */
  CREATE(StandardWatchEventKinds.ENTRY_CREATE),

  /**
   * An existing file or directory was deleted from a watched directory.
   * Maps to {@link StandardWatchEventKinds#ENTRY_DELETE}.
   */
  DELETE(StandardWatchEventKinds.ENTRY_DELETE),

  /**
   * An existing file or directory within a watched directory was modified.
   * Maps to {@link StandardWatchEventKinds#ENTRY_MODIFY}.
   */
  MODIFY(StandardWatchEventKinds.ENTRY_MODIFY);

  /**
   * The NIO watch-event kind that corresponds to this heap event type.
   */
  private final WatchEvent.Kind<?> kind;

  /**
   * Constructs a heap event type that wraps the given NIO watch-event kind.
   *
   * @param kind the {@link WatchEvent.Kind} that this enum constant represents
   */
  HeapEventType (WatchEvent.Kind<?> kind) {

    this.kind = kind;
  }

  /**
   * Returns the NIO {@link WatchEvent.Kind} that corresponds to this heap event type.
   *
   * <p>The returned value can be passed directly to NIO watch-service APIs when
   * translating a {@link HeapEvent} into a standard file-system watch event.
   *
   * @return the corresponding {@link WatchEvent.Kind}; never {@code null}
   */
  public WatchEvent.Kind<?> getKind () {

    return kind;
  }
}
