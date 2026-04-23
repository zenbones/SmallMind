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

import java.util.EventListener;

/**
 * Callback interface for objects that wish to be notified of changes within the
 * ephemeral heap file-system tree.
 *
 * <p>Implementations are registered with a {@link HeapNode} via
 * {@link HeapNode#registerListener(HeapEventListener)} and deregistered via
 * {@link HeapNode#unregisterListener(HeapEventListener)}. When a node is created,
 * deleted, or modified, the originating node calls {@link HeapNode#bubble(HeapEvent)},
 * which invokes {@link #handle(HeapEvent)} on every registered listener and then
 * propagates the event upward to the parent node.
 *
 * <p>Typical consumers translate received {@link HeapEvent}s into
 * {@link java.nio.file.WatchEvent}s for delivery through the NIO
 * {@link java.nio.file.WatchService} subsystem.
 *
 * @see HeapEvent
 * @see HeapNode#registerListener(HeapEventListener)
 * @see HeapNode#unregisterListener(HeapEventListener)
 */
public interface HeapEventListener extends EventListener {

  /**
   * Invoked when a heap node change event has been bubbled to this listener.
   *
   * @param heapEvent the event describing the type of change and the affected path;
   *                  never {@code null}
   */
  void handle (HeapEvent heapEvent);
}
