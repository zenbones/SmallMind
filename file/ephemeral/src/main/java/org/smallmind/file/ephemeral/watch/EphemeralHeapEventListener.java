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

import org.smallmind.file.ephemeral.heap.HeapEvent;
import org.smallmind.file.ephemeral.heap.HeapEventListener;

/**
 * Bridges heap-level file system events to the {@link EphemeralWatchService} so that
 * registered watch-key clients receive {@link java.nio.file.WatchEvent} notifications.
 *
 * <p>An instance of this listener is created by {@link EphemeralWatchService} and attached
 * to the underlying {@link org.smallmind.file.ephemeral.EphemeralFileStore} heap for each
 * path that has at least one active watch key. When the heap raises a
 * {@link HeapEvent}, this listener translates it into the corresponding
 * {@link java.nio.file.WatchEvent.Kind} and delegates to
 * {@link EphemeralWatchService#fire(org.smallmind.file.ephemeral.EphemeralPath, java.nio.file.WatchEvent.Kind)}.
 */
public class EphemeralHeapEventListener implements HeapEventListener {

  /**
   * The watch service to which translated events are forwarded.
   */
  private final EphemeralWatchService watchService;

  /**
   * Creates a listener bound to the given watch service.
   *
   * @param watchService the {@link EphemeralWatchService} that will receive forwarded events;
   *                     must not be {@code null}
   */
  public EphemeralHeapEventListener (EphemeralWatchService watchService) {

    this.watchService = watchService;
  }

  /**
   * Handles a heap event by translating it into a {@link java.nio.file.WatchEvent.Kind} and
   * forwarding it to the bound {@link EphemeralWatchService}.
   *
   * <p>The event kind is obtained via {@code heapEvent.getType().getKind()}, and the affected
   * path is obtained via {@code heapEvent.getPath()}. Both are passed directly to
   * {@link EphemeralWatchService#fire(org.smallmind.file.ephemeral.EphemeralPath, java.nio.file.WatchEvent.Kind)}.
   *
   * @param heapEvent the event emitted by the in-memory heap; must not be {@code null}
   */
  @Override
  public void handle (HeapEvent heapEvent) {

    watchService.fire(heapEvent.getPath(), heapEvent.getType().getKind());
  }
}
