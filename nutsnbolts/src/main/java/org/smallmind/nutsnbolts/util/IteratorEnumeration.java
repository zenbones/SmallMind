package org.smallmind.nutsnbolts.util;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorEnumeration<T> implements Enumeration<T> {

  private Iterator<T> internalIterator;

  public IteratorEnumeration (Iterator<T> internalIterator) {

    this.internalIterator = internalIterator;
  }

  @Override
  public boolean hasMoreElements () {

    return internalIterator.hasNext();
  }

  @Override
  public T nextElement () {

    return internalIterator.next();
  }
}
