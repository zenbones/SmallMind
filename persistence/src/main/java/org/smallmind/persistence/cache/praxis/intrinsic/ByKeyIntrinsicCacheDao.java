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
package org.smallmind.persistence.cache.praxis.intrinsic;

import java.io.Serializable;
import java.util.Comparator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.AbstractCacheDao;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.praxis.ByKeySingularVector;

/**
 * Cache DAO for thread-safe intrinsic caches where vectors reference instances by durable key.
 */
public class ByKeyIntrinsicCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  /**
   * Creates an intrinsic key-based cache DAO.
   *
   * @param cacheDomain cache domain supplying instance and vector caches
   */
  public ByKeyIntrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

  /**
   * Persists a durable into the instance cache, returning any existing value.
   *
   * @param durableClass managed durable class
   * @param durable      durable to cache
   * @param mode         update mode (ignored for intrinsic caches)
   * @return cached durable or {@code null} when input is null
   */
  public D persist (Class<D> durableClass, D durable, UpdateMode mode) {

    if (durable != null) {

      D cachedDurable;
      DurableKey<I, D> durableKey = new DurableKey<>(durableClass, durable.getId());

      return ((cachedDurable = getInstanceCache(durableClass).putIfAbsent(durableKey.getKey(), durable, 0)) != null) ? cachedDurable : durable;
    }

    return null;
  }

  /**
   * Adds a durable to a cached vector if present.
   *
   * @param vectorKey cache key describing the vector
   * @param durable   durable to add
   */
  public void updateInVector (VectorKey<D> vectorKey, D durable) {

    if (durable != null) {

      DurableVector<I, D> vector;

      if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
        vector.add(durable);
      }
    }
  }

  /**
   * Removes a durable from a cached vector, deleting the vector when it is singular.
   *
   * @param vectorKey cache key describing the vector
   * @param durable   durable to remove
   */
  public void removeFromVector (VectorKey<D> vectorKey, D durable) {

    if (durable != null) {

      DurableVector<I, D> vector;

      if ((vector = getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey())) != null) {
        if (vector.isSingular()) {
          deleteVector(vectorKey);
        } else {
          vector.remove(durable);
        }
      }
    }
  }

  /**
   * Migrates a vector into the intrinsic key-based format expected by this DAO.
   *
   * @param managedClass durable class stored in the vector
   * @param vector       vector to migrate
   * @return migrated vector
   */
  public DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector) {

    if (vector.isSingular()) {
      if (!(vector instanceof ByKeySingularVector)) {

        return new ByKeySingularVector<>(new DurableKey<>(managedClass, vector.head().getId()), vector.getTimeToLiveSeconds());
      }

      return vector;
    } else {
      if (!(vector instanceof ByKeyIntrinsicVector)) {

        return new ByKeyIntrinsicVector<>(managedClass, vector.asBestEffortPreFetchedList(), vector.getComparator(), vector.getMaxSize(), vector.getTimeToLiveSeconds(), vector.isOrdered());
      }

      return vector;
    }
  }

  /**
   * Creates a single-element vector backed by a durable key.
   *
   * @param vectorKey         cache key describing the vector
   * @param durable           durable to reference
   * @param timeToLiveSeconds TTL for the vector
   * @return new singular vector
   */
  public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds) {

    return new ByKeySingularVector<>(new DurableKey<>(vectorKey.getElementClass(), durable.getId()), timeToLiveSeconds);
  }

  /**
   * Creates a vector backed by durable keys for the supplied elements.
   *
   * @param vectorKey         cache key describing the vector
   * @param elementIter       iterable of durables to include
   * @param comparator        comparator used for ordering; {@code null} for natural order
   * @param maxSize           maximum number of elements to retain
   * @param timeToLiveSeconds TTL for the vector
   * @param ordered           whether to maintain sorted order
   * @return new intrinsic vector
   */
  public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    return new ByKeyIntrinsicVector<>(vectorKey.getElementClass(), elementIter, comparator, maxSize, timeToLiveSeconds, ordered);
  }
}
