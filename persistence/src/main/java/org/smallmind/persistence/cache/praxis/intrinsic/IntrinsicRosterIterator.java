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

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * {@link ListIterator} that traverses an {@link IntrinsicRoster} using direct node links, acquiring
 * the roster's write lock only when mutating operations ({@link #remove()}, {@link #add(Object)}) are called.
 *
 * @param <T> the element type
 */
public class IntrinsicRosterIterator<T> implements ListIterator<T> {

  private final IntrinsicRoster<T> concurrentList;
  private IntrinsicRosterNode<T> next;
  private IntrinsicRosterNode<T> prev;
  private IntrinsicRosterNode<T> current;
  private int index;

  /**
   * Constructs an iterator positioned between the supplied nodes.
   *
   * @param concurrentList the backing roster
   * @param prev           the node immediately before the iterator's current position, or {@code null} when at the head
   * @param next           the node immediately after the iterator's current position, or {@code null} when at the tail
   * @param index          the initial value returned by {@link #nextIndex()}
   */
  public IntrinsicRosterIterator (IntrinsicRoster<T> concurrentList, IntrinsicRosterNode<T> prev, IntrinsicRosterNode<T> next, int index) {

    this.concurrentList = concurrentList;
    this.next = next;
    this.prev = prev;
    this.index = index;
  }

  /**
   * Returns {@code true} when a subsequent call to {@link #next()} would succeed.
   *
   * @return {@code true} if there is a next element
   */
  public boolean hasNext () {

    return next != null;
  }

  /**
   * Returns the next element and advances the iterator position.
   *
   * @return the next element
   * @throws NoSuchElementException when no further elements exist
   */
  public T next () {

    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    try {

      return next.getObj();
    } finally {
      current = next;
      prev = next;
      next = concurrentList.getNextInView(next);
      index++;
    }
  }

  /**
   * Returns {@code true} when a subsequent call to {@link #previous()} would succeed.
   *
   * @return {@code true} if there is a previous element
   */
  public boolean hasPrevious () {

    return prev != null;
  }

  /**
   * Returns the previous element and moves the iterator position backward.
   *
   * @return the previous element
   * @throws NoSuchElementException when no previous element exists
   */
  public T previous () {

    if (!hasPrevious()) {
      throw new NoSuchElementException();
    }

    try {

      return prev.getObj();
    } finally {
      current = prev;
      next = prev;
      prev = concurrentList.getPrevInView(prev);
      index--;
    }
  }

  /**
   * Returns the index of the element that would be returned by a subsequent call to {@link #next()}.
   *
   * @return the next index
   */
  public int nextIndex () {

    return index;
  }

  /**
   * Returns the index of the element that would be returned by a subsequent call to {@link #previous()}.
   *
   * @return the previous index
   */
  public int previousIndex () {

    return index - 1;
  }

  /**
   * Replaces the element last returned by {@link #next()} or {@link #previous()}.
   *
   * @param obj the replacement value
   * @throws IllegalStateException when neither {@link #next()} nor {@link #previous()} has been called
   */
  public void set (T obj) {

    if (current == null) {
      throw new IllegalStateException();
    }

    current.setObj(obj);
  }

  /**
   * Removes the element last returned by {@link #next()} or {@link #previous()} from the backing roster.
   *
   * @throws IllegalStateException when neither {@link #next()} nor {@link #previous()} has been called,
   *                               or when {@link #remove()} or {@link #add(Object)} was already called
   */
  public void remove () {

    if (current == null) {
      throw new IllegalStateException();
    }

    concurrentList.getLock().writeLock().lock();
    try {
      concurrentList.removeNode(current);
    } finally {
      concurrentList.getLock().writeLock().unlock();
    }

    current = null;
  }

  /**
   * Inserts a new element into the roster adjacent to the last returned element.
   *
   * @param t the element to insert
   * @throws IllegalStateException when neither {@link #next()} nor {@link #previous()} has been called,
   *                               or when {@link #remove()} or {@link #add(Object)} was already called
   */
  public void add (T t) {

    if (current == null) {
      throw new IllegalStateException();
    }

    concurrentList.getLock().writeLock().lock();
    try {
      concurrentList.add(current, t);
    } finally {
      concurrentList.getLock().writeLock().unlock();
    }

    current = null;
  }
}
