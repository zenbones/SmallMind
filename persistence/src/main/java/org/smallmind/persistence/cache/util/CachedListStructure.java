/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.util;

import org.terracotta.annotations.InstrumentedClass;

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
