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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.smallmind.nutsnbolts.util.SingleItemIterable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;

/**
 * Singular {@link DurableVector} that references a single durable by a {@link DurableKey} and resolves
 * it lazily through the backing {@link ORMDao} on every access.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByKeySingularVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  private transient volatile ORMDao<I, D, ?, ?> ormDao;

  private DurableKey<I, D> durableKey;

  /**
   * Creates a singular vector pointing at the durable identified by the given key.
   *
   * @param durableKey        the key referencing the single durable
   * @param timeToLiveSeconds the TTL for this vector in seconds
   */
  public ByKeySingularVector (DurableKey<I, D> durableKey, int timeToLiveSeconds) {

    super(null, 1, timeToLiveSeconds, false);

    this.durableKey = durableKey;
  }

  /**
   * Lazily resolves and caches the {@link ORMDao} used to load the referenced durable.
   *
   * @return the ORM DAO for the managed durable class
   * @throws CacheOperationException when no DAO is registered for the durable class
   */
  private ORMDao<I, D, ?, ?> getORMDao () {

    if (ormDao == null) {
      if ((ormDao = OrmDaoManager.get(durableKey.getDurableClass())) == null) {
        throw new CacheOperationException("Unable to locate an implementation of ORMDao within DaoManager for the requested durable(%s)", durableKey.getDurableClass().getSimpleName());
      }
    }

    return ormDao;
  }

  /**
   * Hydrates and returns the durable referenced by the stored key.
   *
   * @return the resolved durable
   * @throws CacheOperationException when the durable cannot be found in the backing store
   */
  private D getDurable () {

    D durable;
    ORMDao<I, D, ?, ?> ormDao;

    if ((durable = (ormDao = getORMDao()).get(ormDao.getIdFromString(durableKey.getIdAsString()))) == null) {
      throw new CacheOperationException("Unable to locate the requested durable(%s) instance(%s)", durableKey.getDurableClass().getSimpleName(), durableKey.getIdAsString());
    }

    return durable;
  }

  /**
   * Creates a copy of this vector preserving the referenced key and TTL.
   *
   * @return a new {@link ByKeySingularVector} with the same key and TTL
   */
  public DurableVector<I, D> copy () {

    return new ByKeySingularVector<>(durableKey, getTimeToLiveSeconds());
  }

  /**
   * Returns {@code true} because this vector always holds exactly one element.
   *
   * @return {@code true}
   */
  public boolean isSingular () {

    return true;
  }

  /**
   * Replaces the referenced key when the supplied durable differs from the currently referenced one.
   *
   * @param durable the durable to store
   * @return {@code true} when the stored key is updated
   */
  public synchronized boolean add (D durable) {

    if (!getDurable().equals(durable)) {
      durableKey = new DurableKey<>(durableKey.getDurableClass(), durable.getId());

      return true;
    }

    return false;
  }

  /**
   * Removal is not supported for singular vectors.
   *
   * @param durable unused
   * @return never returns normally
   * @throws UnsupportedOperationException always
   */
  public boolean remove (D durable) {

    throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
  }

  /**
   * Returns the durable referenced by this vector.
   *
   * @return the resolved durable
   */
  public synchronized D head () {

    return getDurable();
  }

  /**
   * Returns a singleton list containing the referenced durable.
   *
   * @return an unmodifiable single-element list
   */
  public synchronized List<D> asBestEffortLazyList () {

    return Collections.singletonList(getDurable());
  }

  /**
   * Returns an iterator that yields the referenced durable exactly once.
   *
   * @return a single-element iterator
   */
  public synchronized Iterator<D> iterator () {

    return new SingleItemIterable<>(getDurable()).iterator();
  }
}
