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

import java.util.LinkedList;
import org.smallmind.file.ephemeral.EphemeralBasicFileAttributes;

/**
 * Abstract base class for all nodes held in the in-memory ephemeral file-system tree.
 *
 * <p>Every node in the heap tree — whether a file ({@link FileNode}) or a directory
 * ({@link DirectoryNode}) — extends this class. A {@code HeapNode} records its parent,
 * its simple name, and a lazily initialised list of {@link HeapEventListener}s. When a
 * change occurs at any node, {@link #bubble(HeapEvent)} propagates the event to locally
 * registered listeners and then up to the parent node, allowing watch-service subscribers
 * to observe changes anywhere beneath a watched directory.
 *
 * <p>All listener-management methods are {@code synchronized} on the node's own monitor
 * to allow safe concurrent registration and event delivery.
 *
 * @see FileNode
 * @see DirectoryNode
 * @see HeapEvent
 * @see HeapEventListener
 */
public abstract class HeapNode {

  /**
   * The NIO file-attribute view maintained for this node.
   * Initialised once at construction and never replaced.
   */
  private final EphemeralBasicFileAttributes attributes;

  /**
   * The parent directory of this node, or {@code null} if this node is the root.
   */
  private final DirectoryNode parent;

  /**
   * The simple name of this node within its parent directory.
   */
  private final String name;

  /**
   * The list of listeners registered on this node. Lazily initialised to avoid allocating a
   * list for nodes that are never watched.
   */
  private LinkedList<HeapEventListener> listenerList;

  /**
   * Creates a new heap node attached to the given parent directory.
   *
   * @param parent the {@link DirectoryNode} that contains this node, or {@code null} when
   *               constructing the root of the file-system tree
   * @param name   the simple (leaf) name of this node
   */
  public HeapNode (DirectoryNode parent, String name) {

    this.parent = parent;
    this.name = name;

    attributes = new EphemeralBasicFileAttributes(this);
  }

  /**
   * Returns the type identifier that distinguishes files from directories.
   *
   * @return the {@link HeapNodeType} for this node; never {@code null}
   */
  public abstract HeapNodeType getType ();

  /**
   * Returns the aggregate byte size represented by this node.
   *
   * <p>For a {@link FileNode} this is the number of bytes written to the file. For a
   * {@link DirectoryNode} this is the recursive sum of all descendant sizes.
   *
   * @return the size in bytes; always &ge; 0
   */
  public abstract long size ();

  /**
   * Returns the parent directory of this node.
   *
   * @return the containing {@link DirectoryNode}, or {@code null} if this node is the root
   */
  public DirectoryNode getParent () {

    return parent;
  }

  /**
   * Returns the simple name of this node within its parent directory.
   *
   * @return the node name; never {@code null}
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the NIO file-attribute view associated with this node.
   *
   * <p>The returned object provides the {@link java.nio.file.attribute.BasicFileAttributes}
   * implementation used by the ephemeral file-system provider.
   *
   * @return the {@link EphemeralBasicFileAttributes} for this node; never {@code null}
   */
  public EphemeralBasicFileAttributes getAttributes () {

    return attributes;
  }

  /**
   * Registers a listener to receive {@link HeapEvent}s that are bubbled through this node.
   *
   * <p>The listener list is lazily created on the first registration. The same listener
   * instance may be added more than once and will then be notified multiple times per event.
   *
   * @param eventListener the {@link HeapEventListener} to add; must not be {@code null}
   */
  public synchronized void registerListener (HeapEventListener eventListener) {

    if (listenerList == null) {
      listenerList = new LinkedList<>();
    }

    listenerList.add(eventListener);
  }

  /**
   * Removes a previously registered event listener from this node.
   *
   * <p>If the listener was registered multiple times, only the first occurrence is removed.
   * If the listener is not currently registered, this method has no effect.
   *
   * @param eventListener the {@link HeapEventListener} to remove; must not be {@code null}
   */
  public synchronized void unregisterListener (HeapEventListener eventListener) {

    listenerList.remove(eventListener);
  }

  /**
   * Delivers a {@link HeapEvent} to every listener registered on this node and then
   * propagates the event to the parent node.
   *
   * <p>Delivery to local listeners happens before propagation to the parent, so a listener
   * at a deeper level in the tree always receives the event before a listener at a shallower
   * level. Propagation stops at the root, where {@link #getParent()} returns {@code null}.
   *
   * @param event the {@link HeapEvent} to deliver; must not be {@code null}
   */
  public synchronized void bubble (HeapEvent event) {

    if (listenerList != null) {
      for (HeapEventListener listener : listenerList) {
        listener.handle(event);
      }
    }

    if (parent != null) {
      parent.bubble(event);
    }
  }
}
