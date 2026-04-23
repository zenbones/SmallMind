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
 * Represents an in-memory directory node in the ephemeral heap file-system tree.
 *
 * <p>A {@code DirectoryNode} maintains a map of named child {@link HeapNode} instances and
 * provides synchronized access to those children. Its reported {@link #size()} is the
 * recursive sum of every descendant's size. The root of the tree is represented by an
 * instance whose parent is {@code null}.
 *
 * @see HeapNode
 * @see FileNode
 */
public class DirectoryNode extends HeapNode {

  /**
   * Map of child node names to their corresponding heap nodes.
   * All access must be performed while holding the monitor of this {@code DirectoryNode}.
   */
  private final HashMap<String, HeapNode> children = new HashMap<>();

  /**
   * Creates a new directory node.
   *
   * @param parent the parent directory that contains this node, or {@code null} when this
   *               node is the root of the file-system tree
   * @param name   the simple name of this directory
   */
  public DirectoryNode (DirectoryNode parent, String name) {

    super(parent, name);
  }

  /**
   * Returns the type identifier for this node.
   *
   * @return {@link HeapNodeType#DIRECTORY}, always
   */
  @Override
  public HeapNodeType getType () {

    return HeapNodeType.DIRECTORY;
  }

  /**
   * Returns whether this directory currently has no children.
   *
   * @return {@code true} if the directory contains no child nodes; {@code false} otherwise
   */
  public synchronized boolean isEmpty () {

    return children.isEmpty();
  }

  /**
   * Removes all child nodes from this directory, leaving it empty.
   */
  public synchronized void clear () {

    children.clear();
  }

  /**
   * Tests whether a child with the given name exists in this directory.
   *
   * @param name the simple name to look up
   * @return {@code true} if a child with that name exists; {@code false} otherwise
   */
  public synchronized boolean exists (String name) {

    return children.containsKey(name);
  }

  /**
   * Retrieves a child node by name.
   *
   * @param name the simple name of the child to retrieve
   * @return the {@link HeapNode} associated with the name, or {@code null} if no such child exists
   */
  public synchronized HeapNode get (String name) {

    return children.get(name);
  }

  /**
   * Adds or replaces a child node in this directory, keyed by the node's own name.
   *
   * @param heapNode the child node to store; its {@link HeapNode#getName()} is used as the key
   * @return this directory node, to allow method chaining
   */
  public synchronized DirectoryNode put (HeapNode heapNode) {

    children.put(heapNode.getName(), heapNode);

    return this;
  }

  /**
   * Removes the child with the given name from this directory.
   *
   * @param name the simple name of the child to remove
   * @return the removed {@link HeapNode}, or {@code null} if no child with that name existed
   */
  public synchronized HeapNode remove (String name) {

    return children.remove(name);
  }

  /**
   * Returns an iterator over the paths of child nodes that are accepted by the given filter.
   *
   * <p>Each child name is resolved against the supplied {@code path} to produce a candidate
   * {@link Path}. The filter's {@link DirectoryStream.Filter#accept(Object)} method is called
   * for each candidate; any {@link IOException} thrown by the filter is silently suppressed and
   * the candidate is excluded from the results.
   *
   * @param path   the parent {@link EphemeralPath} against which each child name is resolved
   * @param filter the filter used to decide which child paths to include
   * @return an iterator over the accepted child paths, in no guaranteed order
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
   * Returns the aggregate size of all descendant nodes.
   *
   * <p>The value is computed on every call by summing the {@link HeapNode#size()} of each
   * direct child; for child directories the computation recurses. There is no caching.
   *
   * @return the total number of bytes consumed by the contents of this directory tree
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
