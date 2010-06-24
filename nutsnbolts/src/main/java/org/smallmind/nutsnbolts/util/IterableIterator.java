package org.smallmind.nutsnbolts.util;

import java.util.Iterator;

public class IterableIterator<T> implements Iterator<T>, Iterable<T> {

   private Iterator<T> internalIterator;

   public IterableIterator (Iterator<T> internalIterator) {

      this.internalIterator = internalIterator;
   }

   public boolean hasNext () {

      return internalIterator.hasNext();
   }

   public T next () {

      return internalIterator.next();
   }

   public void remove () {

      throw new UnsupportedOperationException();
   }

   public Iterator<T> iterator () {

      return internalIterator;
   }
}
