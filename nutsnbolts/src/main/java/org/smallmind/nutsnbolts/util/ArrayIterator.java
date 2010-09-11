package org.smallmind.nutsnbolts.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator<T> implements Iterator<T>, Iterable<T> {

   private T[] array;
   private int index = 0;

   public ArrayIterator (T[] array) {

      this.array = array;
   }

   @Override
   public Iterator<T> iterator () {

      return this;
   }

   @Override
   public synchronized boolean hasNext () {

      return index < array.length;
   }

   @Override
   public synchronized T next () {

      if (!hasNext()) {
         throw new NoSuchElementException();
      }

      return array[index++];
   }

   @Override
   public void remove () {

      throw new UnsupportedOperationException();
   }
}
