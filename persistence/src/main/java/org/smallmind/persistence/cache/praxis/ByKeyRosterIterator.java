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
 * {@link ListIterator} that traverses a key-based roster while transparently hydrating
 * {@link DurableKey} values into {@link Durable} instances via an {@link ORMDao}.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByKeyRosterIterator<I extends Serializable & Comparable<I>, D extends Durable<I>> implements ListIterator<D> {

  private final ORMDao<I, D, ?, ?> ormDao;
  private final ListIterator<DurableKey<I, D>> keyListIterator;

  /**
   * Constructs an iterator that resolves durables via the supplied DAO.
   *
   * @param ormDao          the DAO used to load durable instances from their keys
   * @param keyListIterator the underlying iterator over {@link DurableKey} values
   */
  public ByKeyRosterIterator (ORMDao<I, D, ?, ?> ormDao, ListIterator<DurableKey<I, D>> keyListIterator) {

    this.ormDao = ormDao;
    this.keyListIterator = keyListIterator;
  }

  /**
   * Hydrates a durable from its key using the backing DAO.
   *
   * @param durableKey the key to resolve; returns {@code null} when this parameter is {@code null}
   * @return the hydrated durable
   * @throws CacheOperationException when the durable cannot be found in the backing store
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
   * Returns {@code true} when more elements are available in the forward direction.
   *
   * @return {@code true} if {@link #next()} would succeed
   */
  public boolean hasNext () {

    return keyListIterator.hasNext();
  }

  /**
   * Returns the next raw {@link DurableKey} without hydrating it into a durable.
   *
   * @return the next durable key
   */
  public DurableKey<I, D> nextKey () {

    return keyListIterator.next();
  }

  /**
   * Returns the next element, hydrated from its stored key.
   *
   * @return the next durable
   */
  public D next () {

    return getDurable(keyListIterator.next());
  }

  /**
   * Returns {@code true} when more elements are available in the backward direction.
   *
   * @return {@code true} if {@link #previous()} would succeed
   */
  public boolean hasPrevious () {

    return keyListIterator.hasPrevious();
  }

  /**
   * Returns the previous element, hydrated from its stored key.
   *
   * @return the previous durable
   */
  public D previous () {

    return getDurable(keyListIterator.previous());
  }

  /**
   * Returns the index of the element that would be returned by a subsequent call to {@link #next()}.
   *
   * @return the next index
   */
  public int nextIndex () {

    return keyListIterator.nextIndex();
  }

  /**
   * Returns the index of the element that would be returned by a subsequent call to {@link #previous()}.
   *
   * @return the previous index
   */
  public int previousIndex () {

    return keyListIterator.previousIndex();
  }

  /**
   * Removes the element last returned by {@link #next()} or {@link #previous()} from the underlying key roster.
   */
  public void remove () {

    keyListIterator.remove();
  }

  /**
   * Replaces the last returned element with a key derived from the supplied durable.
   *
   * @param durable the durable whose key will replace the current element
   */
  public void set (D durable) {

    keyListIterator.set(new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }

  /**
   * Inserts a key derived from the supplied durable at the current iterator position.
   *
   * @param durable the durable to add
   */
  public void add (D durable) {

    keyListIterator.add(new DurableKey<I, D>(ormDao.getManagedClass(), durable.getId()));
  }
}
