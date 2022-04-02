package org.smallmind.file.ephemeral.heap;

public abstract class HeapNode {

  private HeapNode parent;
  private String name;

  public abstract long size ();
}
