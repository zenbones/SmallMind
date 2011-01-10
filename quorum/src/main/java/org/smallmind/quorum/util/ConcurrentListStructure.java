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
package org.smallmind.quorum.util;

import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class ConcurrentListStructure<T> {

   private ConcurrentListStructure<T> subStructure;
   private ConcurrentListNode<T> head;
   private ConcurrentListNode<T> tail;
   int size;

   public ConcurrentListStructure () {

      size = 0;
   }

   public ConcurrentListStructure (ConcurrentListStructure<T> subStructure, ConcurrentListNode<T> head, ConcurrentListNode<T> tail, int size) {

      this.subStructure = subStructure;
      this.head = head;
      this.tail = tail;
      this.size = size;
   }

   public ConcurrentListNode<T> getHead () {

      return head;
   }

   public void setHead (ConcurrentListNode<T> head) {

      if ((subStructure != null) && subStructure.isHead(this.head)) {
         subStructure.setHead(head);
      }

      this.head = head;
   }

   public boolean isHead (ConcurrentListNode<T> node) {

      return (head != null) && (node == head);
   }

   public ConcurrentListNode<T> getTail () {

      return tail;
   }

   public void setTail (ConcurrentListNode<T> tail) {

      if ((subStructure != null) && subStructure.isTail(this.tail)) {
         subStructure.setTail(tail);
      }

      this.tail = tail;
   }

   public boolean isTail (ConcurrentListNode<T> node) {

      return (tail != null) && (node == tail);
   }

   public void evaporate (ConcurrentListNode<T> prev, ConcurrentListNode<T> current, ConcurrentListNode<T> next) {

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

      ConcurrentListNode<T> added = new ConcurrentListNode<T>(element, head, tail);

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

   public void reconstitute (ConcurrentListNode<T> added, ConcurrentListNode<T> head, ConcurrentListNode<T> tail) {

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
