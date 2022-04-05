/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class DirectoryNode extends HeapNode {

  private final HashMap<String, HeapNode> children = new HashMap<>();

  public DirectoryNode (DirectoryNode parent, String name) {

    super(parent, name);
  }

  @Override
  public HeapNodeType getType () {

    return HeapNodeType.DIRECTORY;
  }

  public synchronized boolean isEmpty () {

    return children.isEmpty();
  }

  public synchronized boolean exists (String name) {

    return children.containsKey(name);
  }

  public synchronized HeapNode get (String name) {

    return children.get(name);
  }

  public synchronized DirectoryNode put (HeapNode heapNode) {

    children.put(heapNode.getName(), heapNode);

    return this;
  }

  public synchronized HeapNode remove (String name) {

    return children.remove(name);
  }

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

  @Override
  public synchronized long size ()
    throws IOException {

    long size = 0;

    for (HeapNode child : children.values()) {
      size += child.size();
    }

    return size;
  }
}
