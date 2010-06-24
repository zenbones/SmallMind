package org.smallmind.nutsnbolts.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingleItemIterator<T> implements Iterator<T>, Iterable<T> {

  private T item;
  private boolean taken;

  public SingleItemIterator (T item) {

    this.item = item;

    taken = item != null;
  }

  public synchronized boolean hasNext () {

    return !taken;
  }

  public synchronized T next () {

    if (taken) {
      throw new NoSuchElementException();
    }

    taken = true;

    return item;
  }

  public void remove () {

    throw new UnsupportedOperationException();
  }

  public Iterator<T> iterator () {

    return this;
  }
}
