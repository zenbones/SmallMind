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

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class ConcurrentListIterator<T> implements ListIterator<T> {

   private ConcurrentList<T> concurrentList;
   private ConcurrentListNode<T> next;
   private ConcurrentListNode<T> prev;
   private ConcurrentListNode<T> current;
   private int index;

   public ConcurrentListIterator (ConcurrentList<T> concurrentList, ConcurrentListNode<T> prev, ConcurrentListNode<T> next, int index) {

      this.concurrentList = concurrentList;
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
         next = concurrentList.getNextInView(next);
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
         prev = concurrentList.getPrevInView(prev);
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

      concurrentList.getLock().writeLock().lock();
      try {
         concurrentList.removeNode(current);
      }
      finally {
         concurrentList.getLock().writeLock().unlock();
      }

      current = null;
   }

   public void add (T t) {

      if (current == null) {
         throw new IllegalStateException();
      }

      concurrentList.getLock().writeLock().lock();
      try {
         concurrentList.add(current, t);
      }
      finally {
         concurrentList.getLock().writeLock().unlock();
      }

      current = null;
   }
}
