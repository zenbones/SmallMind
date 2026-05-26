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

import org.smallmind.file.ephemeral.EphemeralPath;
import org.smallmind.file.ephemeral.heap.HeapEvent;
import org.smallmind.file.ephemeral.heap.HeapEventListener;

/**
 * Bridges heap-level file system events to the {@link EphemeralWatchService} so that
 * registered watch-key clients receive {@link java.nio.file.WatchEvent} notifications.
 *
 * <p>One instance is created by {@link EphemeralWatchService} per watched path and attached
 * to the heap node at that path in the underlying
 * {@link org.smallmind.file.ephemeral.EphemeralFileStore}. The listener knows its watched
 * path and forwards every heap event bubbled through that node to the service together with
 * the path of the actually changed entry. The service then computes the
 * {@link java.nio.file.WatchEvent#context() context} as the entry path relative to the
 * watched directory.
 */
public class EphemeralHeapEventListener implements HeapEventListener {

  /**
   * The watch service to which translated events are forwarded.
   */
  private final EphemeralWatchService watchService;

  /**
   * The path of the directory at which this listener was registered. Used by the service to
   * route incoming events to the correct set of {@link EphemeralWatchKey}s and to compute the
   * relative {@link java.nio.file.WatchEvent#context() context} of each fired event.
   */
  private final EphemeralPath watchedPath;

  /**
   * Creates a listener bound to the given watch service and watched path.
   *
   * @param watchService the {@link EphemeralWatchService} that will receive forwarded events;
   *                     must not be {@code null}
   * @param watchedPath  the path of the directory at which this listener is registered;
   *                     must not be {@code null}
   */
  public EphemeralHeapEventListener (EphemeralWatchService watchService, EphemeralPath watchedPath) {

    this.watchService = watchService;
    this.watchedPath = watchedPath;
  }

  /**
   * Handles a heap event by forwarding it to the bound {@link EphemeralWatchService} with the
   * listener's own watched path as the routing key and the heap event's affected path as the
   * changed-entry path.
   *
   * @param heapEvent the event emitted by the in-memory heap; must not be {@code null}
   */
  @Override
  public void handle (HeapEvent heapEvent) {

    watchService.fire(watchedPath, heapEvent.getType().getKind(), heapEvent.getPath());
  }
}
