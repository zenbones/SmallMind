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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.smallmind.file.ephemeral.EphemeralPath;

/**
 * Represents an in-memory directory in the ephemeral heap, maintaining children and aggregating size information.
 */
public class DirectoryNode extends HeapNode {

  private final HashMap<String, HeapNode> children = new HashMap<>();

  /**
   * Creates a directory node.
   *
   * @param parent the parent directory, or {@code null} when this is the root
   * @param name   the directory name
   */
  public DirectoryNode (DirectoryNode parent, String name) {

    super(parent, name);
  }

  /**
   * @return {@link HeapNodeType#DIRECTORY}
   */
  @Override
  public HeapNodeType getType () {

    return HeapNodeType.DIRECTORY;
  }

  /**
   * @return {@code true} when the directory holds no children
   */
  public synchronized boolean isEmpty () {

    return children.isEmpty();
  }

  /**
   * Removes all children from this directory.
   */
  public synchronized void clear () {

    children.clear();
  }

  /**
   * Tests whether a child name exists.
   *
   * @param name the name to check
   * @return {@code true} if a child with the supplied name exists
   */
  public synchronized boolean exists (String name) {

    return children.containsKey(name);
  }

  /**
   * Returns a child node by name.
   *
   * @param name the child name
   * @return the node or {@code null} if absent
   */
  public synchronized HeapNode get (String name) {

    return children.get(name);
  }

  /**
   * Adds or replaces a child node.
   *
   * @param heapNode the node to add
   * @return this directory for chaining
   */
  public synchronized DirectoryNode put (HeapNode heapNode) {

    children.put(heapNode.getName(), heapNode);

    return this;
  }

  /**
   * Removes the child with the provided name.
   *
   * @param name the child name
   * @return the removed node or {@code null} if no such child exists
   */
  public synchronized HeapNode remove (String name) {

    return children.remove(name);
  }

  /**
   * Builds an iterator of child paths that satisfy the provided filter.
   *
   * @param path   the parent path to resolve each child against
   * @param filter optional filter to apply to children
   * @return an iterator over accepted child paths
   */
  public synchronized Iterator<Path> iterator (EphemeralPath path, DirectoryStream.Filter<? super Path> filter) {

    LinkedList<Path> pathList = new LinkedList<>();

    for (String name : children.keySet()) {

      Path childPath;

      try {
        if (filter.accept(childPath = path.resolve(name))) {
          pathList.add(childPath);
        }
      } catch (IOException ioException) {
        // nothing to do here
      }
    }

    return pathList.iterator();
  }

  /**
   * @return the aggregated size of all children
   */
  @Override
  public synchronized long size () {

    long size = 0;

    for (HeapNode child : children.values()) {
      size += child.size();
    }

    return size;
  }
}
