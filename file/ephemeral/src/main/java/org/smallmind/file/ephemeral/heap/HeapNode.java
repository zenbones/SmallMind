/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public abstract class HeapNode {

  private final EphemeralBasicFileAttributes attributes;
  private final DirectoryNode parent;
  private final String name;
  private LinkedList<HeapEventListener> listenerList;

  public HeapNode (DirectoryNode parent, String name) {

    this.parent = parent;
    this.name = name;

    attributes = new EphemeralBasicFileAttributes(this);
  }

  public abstract HeapNodeType getType ();

  public abstract long size ();

  public DirectoryNode getParent () {

    return parent;
  }

  public String getName () {

    return name;
  }

  public EphemeralBasicFileAttributes getAttributes () {

    return attributes;
  }

  public synchronized void registerListener (HeapEventListener eventListener) {

    if (listenerList == null) {
      listenerList = new LinkedList<>();
    }

    listenerList.add(eventListener);
  }

  public synchronized void unregisterListener (HeapEventListener eventListener) {

    listenerList.remove(eventListener);
  }

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
