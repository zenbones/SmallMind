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
package org.smallmind.nutsnbolts.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Chains multiple iterators into a single sequential iterator and iterable, advancing to the next constituent iterator when one is exhausted.
 *
 * @param <T> element type
 */
public class MultipleIterator<T> implements Iterator<T>, Iterable<T> {

  private final ArrayList<Iterator<T>> iteratorList = new ArrayList<>();
  private int index = 0;

  /**
   * Appends an iterator to the end of the chain.
   *
   * @param iterator iterator to append
   */
  public void add (Iterator<T> iterator) {

    iteratorList.add(iterator);
  }

  /**
   * Advances the internal cursor past any exhausted iterators at the current position.
   */
  public void done () {

    moveIndex();
  }

  private void moveIndex () {

    while ((index < iteratorList.size()) && (!iteratorList.get(index).hasNext())) {
      index++;
    }
  }

  /**
   * Returns this instance so that it may be used directly in an enhanced-for loop.
   *
   * @return this iterator
   */
  public Iterator<T> iterator () {

    return this;
  }

  /**
   * Returns {@code true} if any remaining iterator in the chain has at least one element.
   *
   * @return {@code true} if more elements are available
   */
  public boolean hasNext () {

    return index < iteratorList.size();
  }

  /**
   * Returns the next element from the current iterator, advancing to the next iterator in the chain as needed.
   *
   * @return next element
   * @throws NoSuchElementException if no elements remain across all chained iterators
   */
  public T next () {

    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    try {
      return iteratorList.get(index).next();
    } finally {
      moveIndex();
    }
  }

  /**
   * Removes the last element returned by {@link #next()} from the underlying iterator that supplied it.
   *
   * @throws IllegalStateException if {@link #next()} has not been called or the chain is exhausted
   */
  public void remove () {

    if (!(index < iteratorList.size())) {
      throw new IllegalStateException("The next() method has not been called");
    }

    iteratorList.get(index).remove();
  }
}
