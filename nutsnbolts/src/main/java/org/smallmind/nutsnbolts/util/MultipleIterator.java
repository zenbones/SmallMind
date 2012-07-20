/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.nutsnbolts.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MultipleIterator<T> implements Iterator<T>, Iterable<T> {

  private Iterator<T>[] iterators;
  private int index = 0;

  public MultipleIterator (Iterable<T>... iterables) {

    iterators = new Iterator[iterables.length];
    for (int count = 0; count < iterables.length; count++) {
      iterators[count] = iterables[count].iterator();
    }

    moveIndex();
  }

  public MultipleIterator (Iterator<T>... iterators) {

    this.iterators = iterators;

    moveIndex();
  }

  private void moveIndex () {

    while ((index < iterators.length) && (!iterators[index].hasNext())) {
      index++;
    }
  }

  public Iterator<T> iterator () {

    return this;
  }

  public boolean hasNext () {

    return index < iterators.length;
  }

  public T next () {

    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    try {
      return iterators[index].next();
    }
    finally {
      moveIndex();
    }
  }

  public void remove () {

    if (!(index < iterators.length)) {
      throw new IllegalStateException("The next() method has not been called");
    }

    iterators[index].remove();
  }
}
