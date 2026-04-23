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
import java.util.NoSuchElementException;

/**
 * An {@link Iterable} that always produces an empty iterator, useful as a null-object placeholder for absent collections.
 *
 * @param <T> the element type
 */
public class EmptyIterable<T> implements Iterable<T> {

  /**
   * Returns an iterator that contains no elements.
   *
   * @return an empty iterator
   */
  public Iterator<T> iterator () {

    return new EmptyIterator<T>();
  }

  private static class EmptyIterator<T> implements Iterator<T> {

    /**
     * Always returns {@code false} because this iterator contains no elements.
     *
     * @return {@code false}
     */
    public synchronized boolean hasNext () {

      return false;
    }

    /**
     * Always throws {@link NoSuchElementException} because this iterator contains no elements.
     *
     * @return never returns normally
     * @throws NoSuchElementException always
     */
    public synchronized T next () {

      throw new NoSuchElementException();
    }

    /**
     * Always throws {@link UnsupportedOperationException} because removal is not supported.
     *
     * @throws UnsupportedOperationException always
     */
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }
}
