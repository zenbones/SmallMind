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

import java.util.Iterator;

/**
 * Adapter that exposes an existing {@link Iterator} as both an {@link Iterator} and an {@link Iterable}.
 *
 * @param <T> the element type
 */
public class IterableIterator<T> implements Iterator<T>, Iterable<T> {

  private final Iterator<T> internalIterator;

  /**
   * Constructs an adapter wrapping the given iterator.
   *
   * @param internalIterator the iterator to wrap; must not be {@code null}
   */
  public IterableIterator (Iterator<T> internalIterator) {

    this.internalIterator = internalIterator;
  }

  /**
   * Delegates to the wrapped iterator's {@link Iterator#hasNext()} method.
   *
   * @return {@code true} if there are more elements
   */
  public boolean hasNext () {

    return internalIterator.hasNext();
  }

  /**
   * Delegates to the wrapped iterator's {@link Iterator#next()} method.
   *
   * @return the next element
   */
  public T next () {

    return internalIterator.next();
  }

  /**
   * Always throws {@link UnsupportedOperationException} because this adapter does not support removal.
   *
   * @throws UnsupportedOperationException always
   */
  public void remove () {

    throw new UnsupportedOperationException();
  }

  /**
   * Returns the wrapped iterator, enabling use of this adapter in enhanced for-loops.
   *
   * @return the underlying iterator
   */
  public Iterator<T> iterator () {

    return internalIterator;
  }
}
