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
 * Base class for nodes held in the in-memory ephemeral file-system tree, providing common metadata and event plumbing.
 */
public abstract class HeapNode {

  private final EphemeralBasicFileAttributes attributes;
  private final DirectoryNode parent;
  private final String name;
  private LinkedList<HeapEventListener> listenerList;

  /**
   * Creates a heap node attached to the given parent with the supplied name.
   *
   * @param parent the parent directory node, or {@code null} for the root
   * @param name   the simple name of this node
   */
  public HeapNode (DirectoryNode parent, String name) {

    this.parent = parent;
    this.name = name;

    attributes = new EphemeralBasicFileAttributes(this);
  }

  /**
   * @return the node type identifier
   */
  public abstract HeapNodeType getType ();

  /**
   * @return the aggregate size represented by this node
   */
  public abstract long size ();

  /**
   * @return the parent directory or {@code null} if this node is the root
   */
  public DirectoryNode getParent () {

    return parent;
  }

  /**
   * @return the node name relative to its parent
   */
  public String getName () {

    return name;
  }

  /**
   * @return the file attributes structure maintained for this node
   */
  public EphemeralBasicFileAttributes getAttributes () {

    return attributes;
  }

  /**
   * Registers a listener to receive events bubbled from this node and its descendants.
   *
   * @param eventListener the listener to notify
   */
  public synchronized void registerListener (HeapEventListener eventListener) {

    if (listenerList == null) {
      listenerList = new LinkedList<>();
    }

    listenerList.add(eventListener);
  }

  /**
   * Removes a previously registered event listener.
   *
   * @param eventListener the listener to stop notifying
   */
  public synchronized void unregisterListener (HeapEventListener eventListener) {

    listenerList.remove(eventListener);
  }

  /**
   * Propagates an event to local listeners and up the directory tree.
   *
   * @param event the event to deliver
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
