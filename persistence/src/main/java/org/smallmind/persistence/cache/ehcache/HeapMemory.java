package org.smallmind.persistence.cache.ehcache;

public class HeapMemory {

  private MemoryUnit unit;
  private int size;

  public HeapMemory () {

  }

  public HeapMemory (int size, MemoryUnit unit) {

    this.size = size;
    this.unit = unit;
  }

  public int getSize () {

    return size;
  }

  public void setSize (int size) {

    this.size = size;
  }

  public MemoryUnit getUnit () {

    return unit;
  }

  public void setUnit (MemoryUnit unit) {

    this.unit = unit;
  }

  public String toString () {

    return size + unit.getCode();
  }
}
