package org.smallmind.nutsnbolts.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MultipleIterator<T> implements Iterator<T>, Iterable<T> {

   private Iterator<T>[] iterators;
   private int index = 0;

   public MultipleIterator (Iterator<T>... iterators) {

      this.iterators = iterators;

      moveIndex();
   }

   private void moveIndex () {

      while ((index < iterators.length) && (!iterators[index].hasNext())) {
         index++;
      }
   }

   public Iterator<T> iterator () {

      return this;
   }

   public boolean hasNext () {

      return index < iterators.length;
   }

   public T next () {

      if (!hasNext()) {
         throw new NoSuchElementException();
      }

      try {
         return iterators[index].next();
      }
      finally {
         moveIndex();
      }
   }

   public void remove () {

      if (!(index < iterators.length)) {
         throw new IllegalStateException("The next() method has not been called");
      }

      iterators[index].remove();
   }
}
