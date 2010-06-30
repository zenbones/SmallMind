package org.smallmind.persistence.cache.util;

import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class CachedListStructure<T> {

   private CachedListStructure<T> subStructure;
   private CachedNode<T> head;
   private CachedNode<T> tail;
   int size;

   public CachedListStructure () {

      size = 0;
   }

   public CachedListStructure (CachedListStructure<T> subStructure, CachedNode<T> head, CachedNode<T> tail, int size) {

      this.subStructure = subStructure;
      this.head = head;
      this.tail = tail;
      this.size = size;
   }

   public CachedNode<T> getHead () {

      return head;
   }

   public void setHead (CachedNode<T> head) {

      if ((subStructure != null) && subStructure.isHead(this.head)) {
         subStructure.setHead(head);
      }

      this.head = head;
   }

   public boolean isHead (CachedNode<T> node) {

      return (head != null) && (node == head);
   }

   public CachedNode<T> getTail () {

      return tail;
   }

   public void setTail (CachedNode<T> tail) {

      if ((subStructure != null) && subStructure.isTail(this.tail)) {
         subStructure.setTail(tail);
      }

      this.tail = tail;
   }

   public boolean isTail (CachedNode<T> node) {

      return (tail != null) && (node == tail);
   }

   public void evaporate (CachedNode<T> prev, CachedNode<T> current, CachedNode<T> next) {

      if (subStructure != null) {
         evaporate(prev, current, next);
      }

      if (size == 0) {
         head = prev;
         tail = next;
      }
      else if (head == current) {
         head = next;
      }
      else if (tail == current) {
         tail = prev;
      }
   }

   public void ouroboros (T element) {

      CachedNode<T> added = new CachedNode<T>(element, head, tail);

      if (head != null) {
         head.setNext(added);
      }
      if (tail != null) {
         tail.setPrev(added);
      }

      if (subStructure != null) {
         subStructure.reconstitute(added, head, tail);
      }

      head = tail = added;
      size = 1;
   }

   public void reconstitute (CachedNode<T> added, CachedNode<T> head, CachedNode<T> tail) {

      if (subStructure != null) {
         subStructure.reconstitute(added, head, tail);
      }

      if (head == null) {
         this.head = added;
      }
      if (tail == null) {
         this.tail = added;
      }

      size++;
   }

   public void clear () {

      if (size > 0) {
         head = head.getPrev();
         tail = tail.getNext();

         if (head != null) {
            head.setNext(tail);
         }
         if (tail != null) {
            tail.setPrev(head);
         }

         size = 0;
      }
   }

   public int getSize () {

      return size;
   }

   public void setSize (int size) {

      this.size = size;
   }

   public void incSize () {

      if (subStructure != null) {
         subStructure.incSize();
      }

      size++;
   }

   public void decSize () {

      if (subStructure != null) {
         subStructure.decSize();
      }

      size--;
   }
}
