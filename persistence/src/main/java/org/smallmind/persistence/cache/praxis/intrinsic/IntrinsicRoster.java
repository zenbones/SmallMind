/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.praxis.intrinsic;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.persistence.cache.praxis.Roster;

/**
 * Thread-safe roster implementation backed by a doubly linked list structure. Supports efficient
 * sub-list views that remain synchronized via shared {@link IntrinsicRosterStructure} metadata.
 *
 * @param <T> element type
 */
public class IntrinsicRoster<T> implements Roster<T> {

  private final ReentrantReadWriteLock lock;

  private final IntrinsicRosterStructure<T> structure;

  /**
   * Creates an empty roster.
   */
  public IntrinsicRoster () {

    this(new ReentrantReadWriteLock(), new IntrinsicRosterStructure<T>());
  }

  /**
   * Creates a roster initialized with the contents of the provided collection.
   *
   * @param c initial elements
   */
  public IntrinsicRoster (Collection<? extends T> c) {

    this(new ReentrantReadWriteLock(), new IntrinsicRosterStructure<T>());

    if (!c.isEmpty()) {

      IntrinsicRosterNode<T> added = null;

      for (T element : c) {
        if (added == null) {
          structure.setHead(added = new IntrinsicRosterNode<T>(element, null, null));
        } else {
          added = new IntrinsicRosterNode<T>(element, added, null);
          added.getPrev().setNext(added);
        }
      }

      structure.setTail(added);
      structure.addSize(c.size());
    }
  }

  /**
   * Internal constructor used to build shared roster views backed by the same structure and lock.
   *
   * @param lock      shared read/write lock guarding structural changes
   * @param structure shared structure defining the view bounds
   */
  private IntrinsicRoster (ReentrantReadWriteLock lock, IntrinsicRosterStructure<T> structure) {

    this.lock = lock;
    this.structure = structure;
  }

  /**
   * @return read/write lock guarding roster mutations
   */
  protected ReentrantReadWriteLock getLock () {

    return lock;
  }

  /**
   * Retrieves the next node relative to the supplied node while honoring tail boundaries.
   *
   * @param current node currently in view
   * @return next node or {@code null} when at the tail
   */
  protected IntrinsicRosterNode<T> getNextInView (IntrinsicRosterNode<T> current) {

    lock.readLock().lock();
    try {

      return structure.isTail(current) ? null : current.getNext();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Retrieves the previous node relative to the supplied node while honoring head boundaries.
   *
   * @param current node currently in view
   * @return previous node or {@code null} when at the head
   */
  protected IntrinsicRosterNode<T> getPrevInView (IntrinsicRosterNode<T> current) {

    lock.readLock().lock();
    try {

      return structure.isHead(current) ? null : current.getPrev();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return number of elements in the roster
   */
  public int size () {

    lock.readLock().lock();
    try {

      return structure.getSize();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return {@code true} when the roster has no elements
   */
  public boolean isEmpty () {

    lock.readLock().lock();
    try {

      return structure.getSize() == 0;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Checks whether the roster contains an element equal to the supplied object.
   *
   * @param obj object to locate
   * @return {@code true} when found
   */
  public boolean contains (Object obj) {

    lock.readLock().lock();
    try {
      if (structure.getSize() > 0) {
        for (IntrinsicRosterNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
          if (current.objEquals(obj)) {

            return true;
          }
        }
      }

      return false;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return array containing roster elements in order
   */
  @Override
  public Object[] toArray () {

    return toArray((Object[])null);
  }

  /**
   * Copies roster elements into the provided array, allocating as needed.
   *
   * @param a   destination array or {@code null} to allocate
   * @param <U> element type
   * @return populated array containing the roster elements
   */
  @Override
  public <U> U[] toArray (U[] a) {

    lock.readLock().lock();
    try {

      Object[] elements = ((a != null) && (a.length >= structure.getSize())) ? a : (Object[])Array.newInstance((a == null) ? Object.class : a.getClass().getComponentType(), structure.getSize());

      if (structure.getSize() > 0) {

        int index = 0;

        for (IntrinsicRosterNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
          elements[index++] = current.getObj();
        }
      }

      if (elements.length > structure.getSize()) {
        elements[structure.getSize()] = null;
      }

      return (U[])elements;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Locates the node at the specified index, traversing from the nearest end.
   *
   * @param index position to resolve
   * @return node at the requested index
   */
  private IntrinsicRosterNode<T> getNode (int index) {

    if ((index < 0) || (index >= structure.getSize())) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }

    IntrinsicRosterNode<T> current;

    if (index <= (structure.getSize() / 2)) {
      current = structure.getHead();
      for (int count = 0; count < index; count++) {
        current = current.getNext();
      }

      return current;
    } else {
      current = structure.getTail();
      for (int count = (structure.getSize() - 1); count > index; count--) {
        current = current.getPrev();
      }

      return current;
    }
  }

  /**
   * Retrieves the first element.
   *
   * @return head element
   * @throws NoSuchElementException when roster is empty
   */
  public T getFirst () {

    lock.readLock().lock();
    try {
      if (structure.getSize() == 0) {
        throw new NoSuchElementException();
      }

      return structure.getHead().getObj();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Retrieves the last element.
   *
   * @return tail element
   * @throws NoSuchElementException when roster is empty
   */
  public T getLast () {

    lock.readLock().lock();
    try {
      if (structure.getSize() == 0) {
        throw new NoSuchElementException();
      }

      return structure.getTail().getObj();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns the element at the specified index.
   *
   * @param index position to retrieve
   * @return element at the index
   */
  public T get (int index) {

    lock.readLock().lock();
    try {

      return getNode(index).getObj();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Replaces the element at the specified index.
   *
   * @param index   position to replace
   * @param element new element
   * @return previous element
   */
  public T set (int index, T element) {

    lock.readLock().lock();
    try {

      IntrinsicRosterNode<T> current;
      T value;

      value = (current = getNode(index)).getObj();
      current.setObj(element);

      return value;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Inserts a new element before the supplied node, updating head references and tracked size.
   *
   * @param next    node that will follow the inserted element
   * @param element element to insert
   */
  protected void add (IntrinsicRosterNode<T> next, T element) {

    IntrinsicRosterNode<T> prev = next.getPrev();
    IntrinsicRosterNode<T> added;

    next.setPrev(added = new IntrinsicRosterNode<>(element, prev, next));
    if (prev != null) {
      prev.setNext(added);
    }
    if (structure.isHead(next)) {
      structure.setHead(added);
    }

    structure.incSize();
  }

  /**
   * Inserts an element at the front of the roster.
   *
   * @param element element to add
   */
  public void addFirst (T element) {

    lock.writeLock().lock();
    try {
      if (structure.getSize() == 0) {
        structure.ouroboros(element);
      } else {
        add(structure.getHead(), element);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Appends an element to the end of the roster.
   *
   * @param element element to add
   */
  public void addLast (T element) {

    lock.writeLock().lock();
    try {
      if (structure.getSize() == 0) {
        structure.ouroboros(element);
      } else {

        IntrinsicRosterNode<T> end;
        IntrinsicRosterNode<T> added = new IntrinsicRosterNode<>(element, structure.getTail(), end = structure.getTail().getNext());

        if (end != null) {
          end.setPrev(added);
        }
        structure.getTail().setNext(added);
        structure.setTail(added);

        structure.incSize();
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Adds an element to the end of the roster.
   *
   * @param element element to add
   * @return always {@code true}
   */
  public boolean add (T element) {

    addLast(element);

    return true;
  }

  /**
   * Inserts an element at the specified index.
   *
   * @param index   insertion position
   * @param element element to add
   */
  public void add (int index, T element) {

    lock.writeLock().lock();
    try {
      if (index == structure.getSize()) {
        addLast(element);
      } else {
        add(getNode(index), element);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Unlinks the provided node from the roster and adjusts boundary pointers and size.
   *
   * @param current node to remove
   */
  protected void removeNode (IntrinsicRosterNode<T> current) {

    IntrinsicRosterNode<T> prev = current.getPrev();
    IntrinsicRosterNode<T> next = current.getNext();

    if (prev != null) {
      prev.setNext(next);
    }
    if (next != null) {
      next.setPrev(prev);
    }

    structure.decSize();
    structure.evaporate(prev, current, next);
  }

  /**
   * Removes and returns the first element.
   *
   * @return removed element
   * @throws NoSuchElementException when roster is empty
   */
  public T removeFirst () {

    lock.writeLock().lock();
    try {
      if (structure.getSize() == 0) {
        throw new NoSuchElementException();
      } else {

        T element = structure.getHead().getObj();

        removeNode(structure.getHead());

        return element;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Removes and returns the last element.
   *
   * @return removed element
   * @throws NoSuchElementException when roster is empty
   */
  public T removeLast () {

    lock.writeLock().lock();
    try {
      if (structure.getSize() == 0) {
        throw new NoSuchElementException();
      } else {

        T element = structure.getTail().getObj();

        removeNode(structure.getTail());

        return element;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Removes the first occurrence of the specified object.
   *
   * @param o object to remove
   * @return {@code true} when an element was removed
   */
  public boolean remove (Object o) {

    lock.writeLock().lock();
    try {
      for (IntrinsicRosterNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
        if (current.objEquals(o)) {
          removeNode(current);

          return true;
        }
      }

      return false;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Removes and returns the element at the given index.
   *
   * @param index index of the element to remove
   * @return removed element
   */
  public T remove (int index) {

    lock.writeLock().lock();
    try {

      IntrinsicRosterNode<T> current;

      removeNode(current = getNode(index));

      return current.getObj();
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Determines whether all elements of the provided collection are present.
   *
   * @param c collection to check
   * @return {@code true} when every element exists in the roster
   */
  public boolean containsAll (Collection<?> c) {

    if (c.isEmpty()) {

      return true;
    }

    HashSet<?> checkSet = new HashSet<Object>(c);

    lock.readLock().lock();
    try {
      for (IntrinsicRosterNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
        checkSet.remove(current.getObj());
        if (checkSet.isEmpty()) {
          return true;
        }
      }

      return false;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Appends all elements from the provided collection.
   *
   * @param c elements to add
   * @return {@code true} when the roster changes
   */
  public boolean addAll (Collection<? extends T> c) {

    if (!c.isEmpty()) {
      lock.writeLock().lock();
      try {
        for (T element : c) {
          addLast(element);
        }

        return true;
      } finally {
        lock.writeLock().unlock();
      }
    }

    return false;
  }

  /**
   * Inserts all elements from the collection starting at the given index.
   *
   * @param index insertion position
   * @param c     elements to add
   * @return {@code true} when the roster changes
   */
  public boolean addAll (int index, Collection<? extends T> c) {

    if (!c.isEmpty()) {
      lock.writeLock().lock();
      try {

        IntrinsicRosterNode<T> next = getNode(index);

        for (T element : c) {
          add(next, element);
        }

        return true;
      } finally {
        lock.writeLock().unlock();
      }
    }

    return false;
  }

  /**
   * Removes all elements contained in the provided collection.
   *
   * @param c elements to remove
   * @return {@code true} when the roster changes
   */
  public boolean removeAll (Collection<?> c) {

    if (c.isEmpty()) {

      return false;
    }

    HashSet<?> checkSet = new HashSet<Object>(c);
    boolean changed = false;

    lock.writeLock().lock();
    try {
      for (IntrinsicRosterNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
        if (checkSet.contains(current.getObj())) {
          removeNode(current);
          changed = true;
        }
      }

      return changed;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Retains only elements contained in the provided collection.
   *
   * @param c elements to retain
   * @return {@code true} when the roster changes
   */
  public boolean retainAll (Collection<?> c) {

    if (c.isEmpty()) {

      return false;
    }

    HashSet<?> checkSet = new HashSet<Object>(c);
    boolean changed = false;

    lock.writeLock().lock();
    try {
      for (IntrinsicRosterNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
        if (!checkSet.contains(current.getObj())) {
          removeNode(current);
          changed = true;
        }
      }

      return changed;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Removes all elements from the roster.
   */
  public void clear () {

    lock.writeLock().lock();
    try {
      structure.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Finds the index of the first occurrence of the given object.
   *
   * @param o object to locate
   * @return index or {@code -1} when not found
   */
  public int indexOf (Object o) {

    lock.readLock().lock();
    try {

      int index = 0;

      for (IntrinsicRosterNode<T> current = structure.getHead(); current != null; current = getNextInView(current)) {
        if (current.objEquals(o)) {

          return index;
        }

        index++;
      }

      return -1;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Finds the index of the last occurrence of the given object.
   *
   * @param o object to locate
   * @return index or {@code -1} when not found
   */
  public int lastIndexOf (Object o) {

    lock.readLock().lock();
    try {

      int index = structure.getSize() - 1;

      for (IntrinsicRosterNode<T> current = structure.getTail(); current != null; current = getPrevInView(current)) {
        if (current.objEquals(o)) {

          return index;
        }

        index--;
      }

      return -1;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * @return iterator over the roster elements
   */
  public Iterator<T> iterator () {

    return listIterator();
  }

  /**
   * @return list iterator starting at the head of the roster
   */
  public ListIterator<T> listIterator () {

    lock.readLock().lock();
    try {

      return new IntrinsicRosterIterator<>(this, null, (structure.getSize() == 0) ? null : structure.getHead(), 0);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns a list iterator starting at the given index.
   *
   * @param index starting position
   * @return positioned list iterator
   */
  public ListIterator<T> listIterator (int index) {

    lock.readLock().lock();
    try {
      if (index > structure.getSize()) {
        throw new IndexOutOfBoundsException(String.valueOf(index));
      } else if (index == structure.getSize()) {
        return new IntrinsicRosterIterator<>(this, (structure.getSize() == 0) ? null : structure.getTail(), null, index);
      } else {

        IntrinsicRosterNode<T> current = getNode(index);

        return new IntrinsicRosterIterator<>(this, getPrevInView(current), current, index);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Creates a sublist view backed by the same underlying structure.
   *
   * @param fromIndex start index inclusive
   * @param toIndex   end index exclusive
   * @return roster representing the requested range
   */
  public List<T> subList (int fromIndex, int toIndex) {

    if (fromIndex > toIndex) {
      throw new IndexOutOfBoundsException(fromIndex + " > " + toIndex);
    }

    lock.readLock().lock();
    try {

      return new IntrinsicRoster<>(lock, new IntrinsicRosterStructure<T>(structure, getNode(fromIndex), (fromIndex == toIndex) ? getNode(fromIndex).getNext() : getNode(toIndex - 1), toIndex - fromIndex));
    } finally {
      lock.readLock().unlock();
    }
  }
}
