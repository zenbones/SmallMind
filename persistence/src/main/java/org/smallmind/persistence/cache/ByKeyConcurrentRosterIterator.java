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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.orm.ORMDao;

public class ByKeyConcurrentRosterIterator<I extends Serializable & Comparable<I>, D extends Durable<I>> implements ListIterator<D> {

  private ORMDao<I, D> ormDao;
  private ListIterator<DurableKey<I, D>> keyListIterator;
  private DurableKey<I, D> nextKey;
  private DurableKey<I, D> prevKey;
  private int nextIndex;
  private int prevIndex;

  public ByKeyConcurrentRosterIterator (ORMDao<I, D> ormDao, ListIterator<DurableKey<I, D>> keyListIterator) {

    this.ormDao = ormDao;
    this.keyListIterator = keyListIterator;

    setTrackingValues();
  }

  private D getDurable (DurableKey<I, D> durableKey) {

    if (durableKey == null) {

      return null;
    }

    int equalsPos;

    if ((equalsPos = durableKey.getKey().indexOf('=')) < 0) {
      throw new CacheOperationException("Invalid durable key(%s)", durableKey);
    }

    return ormDao.get(ormDao.getIdFromString(durableKey.getKey().substring(equalsPos + 1)));
  }

  private void setTrackingValues () {

    nextKey = keyListIterator.hasNext() ? keyListIterator.next() : null;
    nextIndex = keyListIterator.nextIndex();
    prevKey = keyListIterator.hasPrevious() ? keyListIterator.previous() : null;
    prevIndex = keyListIterator.previousIndex();
  }

  public boolean hasNext () {

    return nextKey != null;
  }

  public D next () {

    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    D nextDurable = getDurable(nextKey);

    setTrackingValues();

    return nextDurable;
  }

  public boolean hasPrevious () {

    return prevKey != null;
  }

  public D previous () {

    if (!hasPrevious()) {
      throw new NoSuchElementException();
    }

    D prevDurable = getDurable(prevKey);

    setTrackingValues();

    return prevDurable;
  }

  public int nextIndex () {

    return nextIndex;
  }

  public int previousIndex () {

    return prevIndex;
  }

  public void remove () {

    keyListIterator.remove();
  }

  public void set (D durable) {

    keyListIterator.set(new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }

  public void add (D durable) {

    keyListIterator.add(new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }
}
