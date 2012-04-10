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
package org.smallmind.persistence.cache.praxis;

import java.io.Serializable;
import java.util.ListIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.orm.ORMDao;

public class ByKeyRosterIterator<I extends Serializable & Comparable<I>, D extends Durable<I>> implements ListIterator<D> {

  private ORMDao<I, D> ormDao;
  private ListIterator<DurableKey<I, D>> keyListIterator;

  public ByKeyRosterIterator (ORMDao<I, D> ormDao, ListIterator<DurableKey<I, D>> keyListIterator) {

    this.ormDao = ormDao;
    this.keyListIterator = keyListIterator;
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

  public boolean hasNext () {

    return keyListIterator.hasNext();
  }

  public DurableKey<I, D> nextKey () {

    return keyListIterator.next();
  }

  public D next () {

    return getDurable(keyListIterator.next());
  }

  public boolean hasPrevious () {

    return keyListIterator.hasPrevious();
  }

  public D previous () {

    return getDurable(keyListIterator.previous());
  }

  public int nextIndex () {

    return keyListIterator.nextIndex();
  }

  public int previousIndex () {

    return keyListIterator.previousIndex();
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
