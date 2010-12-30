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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class CachedList<T> implements List<T> {

   private final ReentrantReadWriteLock lock;

   private CachedListStructure<T> structure;

   public CachedList () {

      this(new ReentrantReadWriteLock(), new CachedListStructure<T>());
   }

   public CachedList (Collection<? extends T> c) {

      this(new ReentrantReadWriteLock(), new CachedListStructure<T>());

      if (!c.isEmpty()) {

         CachedNode<T> added = null;

         for (T element : c) {
            if (added == null) {
               structure.setHead(added = new CachedNode<T>(element, null, null));
            }
            else {
               added = new CachedNode<T>(element, added, null);
               added.getPrev().setNext(added);
            }
         }

         structure.setTail(added);
         structure.setSize(c.size());
      }
   }

   public CachedList (ReentrantReadWriteLock lock, CachedListStructure<T> structure) {

      this.lock = lock;
      this.structure = structure;
   }

   protected ReentrantReadWriteLock getLock () {

      return lock;
   }

   protected CachedNode<T> getNextInView (CachedNode<T> current) {

      lock.readLock().lock();
      try {

         return structure.isTail(current) ? null : current.getNext();
      }
      finally {
         lock.readLock().unlock();
      }
   }

   protected CachedNode<T> getPrevInView (CachedNode<T> current) {

      lock.readLock().lock();
      try {

         return structure.isHead(current) ? null : current.getPrev();
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public int size () {

      lock.readLock().lock();
      try {

         return structure.getSize();
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public boolean isEmpty () {

      lock.readLock().lock();
      try {

         return structure.getSize() == 0;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public boolean contains (Object obj) {

      lock.readLock().lock();
      try {
         if (structure.getSize() > 0) {
            for (CachedNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
               if (current.objEquals(obj)) {

                  return true;
               }
            }
         }

         return false;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public Object[] toArray () {

      return toArray(null);
   }

   public <U> U[] toArray (U[] a) {

      lock.readLock().lock();
      try {

         Object[] elements = ((a != null) && (a.length >= structure.getSize())) ? a : (Object[])Array.newInstance((a == null) ? Object.class : a.getClass().getComponentType(), structure.getSize());

         if (structure.getSize() > 0) {

            int index = 0;

            for (CachedNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
               elements[index++] = current.getObj();
            }
         }

         if (elements.length > structure.getSize()) {
            elements[structure.getSize()] = null;
         }

         return (U[])elements;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   private CachedNode<T> getNode (int index) {

      if ((index < 0) || (index >= structure.getSize())) {
         throw new IndexOutOfBoundsException(String.valueOf(index));
      }

      CachedNode<T> current;

      if (index <= (structure.getSize() / 2)) {
         current = structure.getHead();
         for (int count = 0; count < index; count++) {
            current = current.getNext();
         }

         return current;
      }
      else {
         current = structure.getTail();
         for (int count = (structure.getSize() - 1); count > index; count--) {
            current = current.getPrev();
         }

         return current;
      }
   }

   public T getFirst () {

      lock.readLock().lock();
      try {
         if (structure.getSize() == 0) {
            throw new NoSuchElementException();
         }

         return structure.getHead().getObj();
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public T getLast () {

      lock.readLock().lock();
      try {
         if (structure.getSize() == 0) {
            throw new NoSuchElementException();
         }

         return structure.getTail().getObj();
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public T get (int index) {

      lock.readLock().lock();
      try {

         return getNode(index).getObj();
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public T set (int index, T element) {

      lock.readLock().lock();
      try {

         CachedNode<T> current;
         T value;

         value = (current = getNode(index)).getObj();
         current.setObj(element);

         return value;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   protected void add (CachedNode<T> next, T element) {

      CachedNode<T> prev = next.getPrev();
      CachedNode<T> added;

      next.setPrev(added = new CachedNode<T>(element, prev, next));
      if (prev != null) {
         prev.setNext(added);
      }
      if (structure.isHead(next)) {
         structure.setHead(added);
      }

      structure.incSize();
   }

   public void addFirst (T element) {

      lock.writeLock().lock();
      try {
         if (structure.getSize() == 0) {
            structure.ouroboros(element);
         }
         else {
            add(structure.getHead(), element);
         }
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public void addLast (T element) {

      lock.writeLock().lock();
      try {
         if (structure.getSize() == 0) {
            structure.ouroboros(element);
         }
         else {

            CachedNode<T> end;
            CachedNode<T> added = new CachedNode<T>(element, structure.getTail(), end = structure.getTail().getNext());

            if (end != null) {
               end.setPrev(added);
            }
            structure.getTail().setNext(added);
            structure.setTail(added);

            structure.incSize();
         }
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public boolean add (T element) {

      addLast(element);

      return true;
   }

   public void add (int index, T element) {

      lock.writeLock().lock();
      try {
         if (index == structure.getSize()) {
            addLast(element);
         }
         else {
            add(getNode(index), element);
         }
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   protected void removeNode (CachedNode<T> current) {

      CachedNode<T> prev = current.getPrev();
      CachedNode<T> next = current.getNext();

      if (prev != null) {
         prev.setNext(next);
      }
      if (next != null) {
         next.setPrev(prev);
      }

      structure.decSize();

      if (structure.getSize() == 0) {
         structure.evaporate(prev, current, next);
      }
      else if (structure.isHead(current)) {
         structure.setHead(next);
      }
      else if (structure.isTail(current)) {
         structure.setTail(prev);
      }
   }

   public T removeFirst () {

      lock.writeLock().lock();
      try {
         if (structure.getSize() == 0) {
            throw new NoSuchElementException();
         }
         else {

            T element = structure.getHead().getObj();

            removeNode(structure.getHead());

            return element;
         }
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public T removeLast () {

      lock.writeLock().lock();
      try {
         if (structure.getSize() == 0) {
            throw new NoSuchElementException();
         }
         else {

            T element = structure.getTail().getObj();

            removeNode(structure.getTail());

            return element;
         }
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public boolean remove (Object o) {

      lock.writeLock().lock();
      try {
         for (CachedNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
            if (current.objEquals(o)) {
               removeNode(current);

               return true;
            }
         }

         return false;
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public T remove (int index) {

      lock.writeLock().lock();
      try {

         CachedNode<T> current;

         removeNode(current = getNode(index));

         return current.getObj();
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public boolean containsAll (Collection<?> c) {

      if (c.isEmpty()) {

         return true;
      }

      HashSet<?> checkSet = new HashSet(c);

      lock.readLock().lock();
      try {
         for (CachedNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
            checkSet.remove(current.getObj());
            if (checkSet.isEmpty()) {
               return true;
            }
         }

         return false;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public boolean addAll (Collection<? extends T> c) {

      if (!c.isEmpty()) {
         lock.writeLock().lock();
         try {
            for (T element : c) {
               addLast(element);
            }

            return true;
         }
         finally {
            lock.writeLock().unlock();
         }
      }

      return false;
   }

   public boolean addAll (int index, Collection<? extends T> c) {

      if (!c.isEmpty()) {
         lock.writeLock().lock();
         try {

            CachedNode<T> next = getNode(index);

            for (T element : c) {
               add(next, element);
            }

            return true;
         }
         finally {
            lock.writeLock().unlock();
         }
      }

      return false;
   }

   public boolean removeAll (Collection<?> c) {

      if (c.isEmpty()) {

         return false;
      }

      HashSet<?> checkSet = new HashSet(c);
      boolean changed = false;

      lock.writeLock().lock();
      try {
         for (CachedNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
            if (checkSet.contains(current.getObj())) {
               removeNode(current);
               changed = true;
            }
         }

         return changed;
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public boolean retainAll (Collection<?> c) {

      if (c.isEmpty()) {

         return false;
      }

      HashSet<?> checkSet = new HashSet(c);
      boolean changed = false;

      lock.writeLock().lock();
      try {
         for (CachedNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
            if (!checkSet.contains(current.getObj())) {
               removeNode(current);
               changed = true;
            }
         }

         return changed;
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public void clear () {

      lock.writeLock().lock();
      try {
         structure.clear();
      }
      finally {
         lock.writeLock().unlock();
      }
   }

   public int indexOf (Object o) {

      lock.readLock().lock();
      try {

         int index = 0;

         for (CachedNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
            if (current.objEquals(o)) {

               return index;
            }

            index++;
         }

         return -1;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public int lastIndexOf (Object o) {

      lock.readLock().lock();
      try {

         int index = structure.getSize() - 1;

         for (CachedNode<T> current = structure.getTail(); current != null; current = getPrevInView(current)) {
            if (current.objEquals(o)) {

               return index;
            }

            index--;
         }

         return -1;
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public Iterator<T> iterator () {

      return listIterator();
   }

   public ListIterator<T> listIterator () {

      lock.readLock().lock();
      try {

         return new CachedListIterator<T>(this, null, structure.getHead(), 0);
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public ListIterator<T> listIterator (int index) {

      lock.readLock().lock();
      try {
         if (index > structure.getSize()) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
         }
         else if (index == structure.getSize()) {
            return new CachedListIterator<T>(this, structure.getTail(), null, index);
         }
         else {

            CachedNode<T> current = getNode(index);

            return new CachedListIterator<T>(this, getPrevInView(current), current, index);
         }
      }
      finally {
         lock.readLock().unlock();
      }
   }

   public List<T> subList (int fromIndex, int toIndex) {

      if (fromIndex > toIndex) {
         throw new IndexOutOfBoundsException(fromIndex + " > " + toIndex);
      }
      lock.readLock().lock();
      try {

         return new CachedList<T>(lock, new CachedListStructure<T>(structure, getNode(fromIndex), getNode(toIndex - 1), toIndex - fromIndex));
      }
      finally {
         lock.readLock().unlock();
      }
   }
}
