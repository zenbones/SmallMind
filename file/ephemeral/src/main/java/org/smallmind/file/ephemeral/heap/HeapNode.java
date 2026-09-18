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
 * <p>Every node in the heap tree — a file ({@link FileNode}), a directory
 * ({@link DirectoryNode}), or a symbolic link ({@link LinkNode}) — extends this class. A
 * {@code HeapNode} records its parent, its simple name, and a lazily initialised list of
 * {@link HeapEventListener}s.
 *
 * <p>A node's parent and name are mutable so that a rename or a move can
 * {@linkplain #relink(DirectoryNode, String) relink} an existing node into its new home. Doing so
 * keeps a directory's entire subtree intact, and makes a move independent of how much lives beneath
 * the node being moved.
 *
 * <p>Event delivery is deliberately <em>not</em> recursive. {@link #fire(HeapEvent)} notifies only
 * the listeners registered on the node it is called upon; deciding who hears about a change is the
 * caller's job. A {@link java.nio.file.WatchService} registration covers the entries of one
 * directory and not its descendants, so the store reports a change by firing on the directory that
 * contains the changed entry.
 *
 * <p>All listener-management methods are {@code synchronized} on the node's own monitor to allow
 * safe concurrent registration and event delivery.
 *
 * @see FileNode
 * @see DirectoryNode
 * @see LinkNode
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
   * The list of listeners registered on this node. Lazily initialised to avoid allocating a
   * list for nodes that are never watched.
   */
  private LinkedList<HeapEventListener> listenerList;

  /**
   * The parent directory of this node, or {@code null} if this node is the root.
   */
  private DirectoryNode parent;

  /**
   * The simple name of this node within its parent directory.
   */
  private String name;

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
   * Returns the type identifier that distinguishes files, directories, and symbolic links.
   *
   * @return the {@link HeapNodeType} for this node; never {@code null}
   */
  public abstract HeapNodeType getType ();

  /**
   * Returns the byte size reported for this node by its file attributes.
   *
   * @return the size in bytes; always &ge; 0
   */
  public abstract long size ();

  /**
   * Returns the parent directory of this node.
   *
   * @return the containing {@link DirectoryNode}, or {@code null} if this node is the root
   */
  public synchronized DirectoryNode getParent () {

    return parent;
  }

  /**
   * Returns the simple name of this node within its parent directory.
   *
   * @return the node name; never {@code null} except for the root
   */
  public synchronized String getName () {

    return name;
  }

  /**
   * Re-parents and renames this node in place.
   *
   * <p>This is how a move is performed. The node itself, and therefore everything beneath it, is
   * preserved; only its position in the tree changes. Callers are responsible for removing the node
   * from its former parent and installing it in its new one.
   *
   * @param parent the directory that will contain this node
   * @param name   the simple name this node will take within that directory
   */
  public synchronized void relink (DirectoryNode parent, String name) {

    this.parent = parent;
    this.name = name;
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
   * Registers a listener to receive {@link HeapEvent}s fired on this node.
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

    if (listenerList != null) {
      listenerList.remove(eventListener);
    }
  }

  /**
   * Delivers a {@link HeapEvent} to every listener registered on this node, and on this node only.
   *
   * <p>Events are not propagated to the parent. A watch registration observes the entries of a
   * single directory, so the store fires on the directory containing a changed entry rather than
   * letting the event climb to the root.
   *
   * @param event the {@link HeapEvent} to deliver; must not be {@code null}
   */
  public synchronized void fire (HeapEvent event) {

    if (listenerList != null) {
      for (HeapEventListener listener : listenerList) {
        listener.handle(event);
      }
    }
  }
}
