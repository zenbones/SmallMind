package org.smallmind.persistence.cache.util;

import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class CachedNode<T> {

   private CachedNode<T> prev;
   private CachedNode<T> next;
   private T obj;

   public CachedNode (T obj, CachedNode<T> prev, CachedNode<T> next) {

      this.obj = obj;
      this.prev = prev;
      this.next = next;
   }

   public synchronized T getObj () {

      return obj;
   }

   public synchronized void setObj (T obj) {

      this.obj = obj;
   }

   public synchronized boolean objEquals (Object something) {

      return (obj == null) ? something == null : obj.equals(something);
   }

   public CachedNode<T> getPrev () {

      return prev;
   }

   public void setPrev (CachedNode<T> prev) {

      this.prev = prev;
   }

   public CachedNode<T> getNext () {

      return next;
   }

   public void setNext (CachedNode<T> next) {

      this.next = next;
   }
}