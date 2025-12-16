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
package org.smallmind.persistence.cache.praxis;

import java.io.Serializable;
import java.util.ListIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.orm.ORMDao;

/**
 * {@link ListIterator} implementation that stores {@link DurableKey} values but exposes hydrated
 * {@link Durable} instances when iterating.
 *
 * @param <I> identifier type
 * @param <D> durable type
 */
public class ByKeyRosterIterator<I extends Serializable & Comparable<I>, D extends Durable<I>> implements ListIterator<D> {

  private final ORMDao<I, D, ?, ?> ormDao;
  private final ListIterator<DurableKey<I, D>> keyListIterator;

  /**
   * Constructs an iterator that resolves durables via the supplied DAO.
   *
   * @param ormDao          DAO used to load durables from keys
   * @param keyListIterator underlying iterator of durable keys
   */
  public ByKeyRosterIterator (ORMDao<I, D, ?, ?> ormDao, ListIterator<DurableKey<I, D>> keyListIterator) {

    this.ormDao = ormDao;
    this.keyListIterator = keyListIterator;
  }

  /**
   * Resolves a durable instance for the supplied key.
   *
   * @param durableKey key identifying the durable
   * @return durable instance or {@code null} when the key is null
   */
  private D getDurable (DurableKey<I, D> durableKey) {

    if (durableKey == null) {

      return null;
    }

    D durable;

    if ((durable = ormDao.get(ormDao.getIdFromString(durableKey.getIdAsString()))) == null) {
      throw new CacheOperationException("Unable to locate the requested durable(%s) instance(%s)", durableKey.getDurableClass().getSimpleName(), durableKey.getIdAsString());
    }

    return durable;
  }

  /**
   * @return {@code true} when another element is available
   */
  public boolean hasNext () {

    return keyListIterator.hasNext();
  }

  /**
   * Returns the next raw durable key without hydrating the durable.
   *
   * @return next durable key
   */
  public DurableKey<I, D> nextKey () {

    return keyListIterator.next();
  }

  /**
   * Returns the next hydrated durable.
   *
   * @return next durable
   */
  public D next () {

    return getDurable(keyListIterator.next());
  }

  /**
   * @return {@code true} when a previous element is available
   */
  public boolean hasPrevious () {

    return keyListIterator.hasPrevious();
  }

  /**
   * Returns the previous hydrated durable.
   *
   * @return previous durable
   */
  public D previous () {

    return getDurable(keyListIterator.previous());
  }

  /**
   * @return index of the element that would be returned by {@link #next()}
   */
  public int nextIndex () {

    return keyListIterator.nextIndex();
  }

  /**
   * @return index of the element that would be returned by {@link #previous()}
   */
  public int previousIndex () {

    return keyListIterator.previousIndex();
  }

  /**
   * Removes the current element from the underlying iterator.
   */
  public void remove () {

    keyListIterator.remove();
  }

  /**
   * Replaces the current element with the supplied durable.
   *
   * @param durable durable to set
   */
  public void set (D durable) {

    keyListIterator.set(new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }

  /**
   * Inserts the supplied durable at the current iterator position.
   *
   * @param durable durable to add
   */
  public void add (D durable) {

    keyListIterator.add(new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }
}
