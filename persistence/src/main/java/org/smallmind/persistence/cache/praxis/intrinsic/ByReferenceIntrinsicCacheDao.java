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
import org.smallmind.persistence.cache.praxis.ByReferenceSingularVector;

/**
 * Cache DAO for thread-safe, in-process caches where vectors hold direct references to cached instances.
 */
public class ByReferenceIntrinsicCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  /**
   * Creates an intrinsic cache DAO using the provided cache domain.
   *
   * @param cacheDomain cache domain supplying instance and vector caches
   */
  public ByReferenceIntrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

  /**
   * Persists a durable into the instance cache, returning any existing value.
   *
   * @param durableClass managed durable class
   * @param durable      durable to cache
   * @param mode         ignored for intrinsic caches; kept for interface compatibility
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
   * Adds a durable to a cached vector if the vector exists.
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
   * Removes a durable from a cached vector, deleting the vector when it becomes singular.
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
   * Converts an arbitrary vector into an intrinsic reference-based vector compatible with this DAO.
   *
   * @param managedClass durable class stored in the vector
   * @param vector       vector to migrate
   * @return migrated vector
   */
  public DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector) {

    if (vector.isSingular()) {
      if (!(vector instanceof ByReferenceSingularVector)) {

        return new ByReferenceSingularVector<>(vector.head(), vector.getTimeToLiveSeconds());
      }

      return vector;
    } else {
      if (!(vector instanceof ByReferenceIntrinsicVector)) {

        return new ByReferenceIntrinsicVector<>(new IntrinsicRoster<>(vector.asBestEffortPreFetchedList()), vector.getComparator(), vector.getMaxSize(), vector.getTimeToLiveSeconds(), vector.isOrdered());
      }

      return vector;
    }
  }

  /**
   * Creates a singular vector containing a cached reference to the supplied durable.
   *
   * @param vectorKey         cache key describing the vector
   * @param durable           durable to cache
   * @param timeToLiveSeconds TTL for the vector
   * @return singular vector referencing a cached durable instance
   */
  public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds) {

    DurableKey<I, D> durableKey;
    D inCacheDurable;

    durableKey = new DurableKey<>(vectorKey.getElementClass(), durable.getId());
    if ((inCacheDurable = getInstanceCache(vectorKey.getElementClass()).putIfAbsent(durableKey.getKey(), durable, 0)) != null) {

      return new ByReferenceSingularVector<>(inCacheDurable, timeToLiveSeconds);
    }

    return new ByReferenceSingularVector<>(durable, timeToLiveSeconds);
  }

  /**
   * Creates a vector containing cached references to the supplied durables.
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

    IntrinsicRoster<D> cacheConsistentElements;
    DurableKey<I, D> durableKey;
    D inCacheDurable;

    cacheConsistentElements = new IntrinsicRoster<>();
    for (D element : elementIter) {
      if (element != null) {

        durableKey = new DurableKey<>(vectorKey.getElementClass(), element.getId());
        if ((inCacheDurable = getInstanceCache(vectorKey.getElementClass()).putIfAbsent(durableKey.getKey(), element, timeToLiveSeconds)) != null) {
          cacheConsistentElements.add(inCacheDurable);
        } else {
          cacheConsistentElements.add(element);
        }
      }
    }

    return new ByReferenceIntrinsicVector<>(cacheConsistentElements, comparator, maxSize, timeToLiveSeconds, ordered);
  }
}
