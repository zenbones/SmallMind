/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class IntrinsicRosterIterator<T> implements ListIterator<T> {

  private final IntrinsicRoster<T> concurrentList;
  private IntrinsicRosterNode<T> next;
  private IntrinsicRosterNode<T> prev;
  private IntrinsicRosterNode<T> current;
  private int index;

  public IntrinsicRosterIterator (IntrinsicRoster<T> concurrentList, IntrinsicRosterNode<T> prev, IntrinsicRosterNode<T> next, int index) {

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
    } finally {
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
    } finally {
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
    } finally {
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
    } finally {
      concurrentList.getLock().writeLock().unlock();
    }

    current = null;
  }
}
