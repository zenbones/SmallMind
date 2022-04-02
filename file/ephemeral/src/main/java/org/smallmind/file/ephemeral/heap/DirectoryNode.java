package org.smallmind.file.ephemeral.heap;

import java.util.HashMap;

public class DirectoryNode extends HeapNode {

  private HashMap<String, HeapNode> children = new HashMap<>();

  @Override
  public synchronized long size () {

    long size = 0;

    for (HeapNode child : children.values()) {
      size += child.size();
    }

    return size;
  }
}
