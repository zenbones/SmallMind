package org.smallmind.persistence.cache.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class CachedListIterator<T> implements ListIterator<T> {

   private CachedList<T> cachedList;
   private CachedNode<T> next;
   private CachedNode<T> prev;
   private CachedNode<T> current;
   private int index;

   public CachedListIterator (CachedList<T> cachedList, CachedNode<T> prev, CachedNode<T> next, int index) {

      this.cachedList = cachedList;
      this.next = next;
      this.prev = prev;
      this.index = index;
   }

   public boolean hasNext () {

      return next != null;
   }

   public T next () {

      if (!hasNext()) {
         throw new NoSuchElementException();
      }

      try {

         return next.getObj();
      }
      finally {
         current = next;
         prev = next;
         next = cachedList.getNextInView(next);
         index++;
      }
   }

   public boolean hasPrevious () {

      return prev != null;
   }

   public T previous () {

      if (!hasPrevious()) {
         throw new NoSuchElementException();
      }

      try {

         return prev.getObj();
      }
      finally {
         current = prev;
         next = prev;
         prev = cachedList.getPrevInView(prev);
         index--;
      }
   }

   public int nextIndex () {

      return index;
   }

   public int previousIndex () {

      return index - 1;
   }

   public void set (T obj) {

      if (current == null) {
         throw new IllegalStateException();
      }

      current.setObj(obj);
   }

   public void remove () {

      if (current == null) {
         throw new IllegalStateException();
      }

      cachedList.getLock().writeLock().lock();
      try {
         cachedList.removeNode(current);
      }
      finally {
         cachedList.getLock().writeLock().unlock();
      }

      current = null;
   }

   public void add (T t) {

      if (current == null) {
         throw new IllegalStateException();
      }

      cachedList.getLock().writeLock().lock();
      try {
         cachedList.add(current, t);
      }
      finally {
         cachedList.getLock().writeLock().unlock();
      }

      current = null;
   }
}
