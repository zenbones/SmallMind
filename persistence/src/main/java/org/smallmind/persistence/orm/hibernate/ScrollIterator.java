/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.orm.hibernate;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.hibernate.ScrollableResults;

public class ScrollIterator<T> implements Iterator<T>, Iterable<T> {

  private ScrollableResults scrollableResults;

  private Class<T> managedClass;

  private boolean more;

  public ScrollIterator (ScrollableResults scrollableResults, Class<T> managedClass) {

    this.scrollableResults = scrollableResults;
    this.managedClass = managedClass;

    more = scrollableResults.first();
  }

  public Iterator<T> iterator () {

    return this;
  }

  public boolean hasNext () {

    return more;
  }

  public T next () {

    if (!more) {
      throw new NoSuchElementException();
    }

    try {

      Object[] result;

      return managedClass.cast((managedClass.isArray() && ((result = scrollableResults.get()).length > 1)) ? result : scrollableResults.get(0));
    }
    finally {
      more = scrollableResults.next();
    }
  }

  public void remove () {

    throw new UnsupportedOperationException();
  }
}