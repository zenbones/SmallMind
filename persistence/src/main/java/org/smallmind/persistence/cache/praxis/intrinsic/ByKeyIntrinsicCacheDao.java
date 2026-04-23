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
 * Cache DAO for thread-safe intrinsic (in-process) caches that stores vector elements as
 * {@link DurableKey} references rather than direct object references.
 * Vector mutations are performed directly on shared in-memory objects without CAS.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByKeyIntrinsicCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  /**
   * Creates a key-based intrinsic cache DAO backed by the provided cache domain.
   *
   * @param cacheDomain the cache domain supplying instance and vector caches
   */
  public ByKeyIntrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

  /**
   * Stores a durable in the instance cache using a put-if-absent strategy, returning any pre-existing entry.
   * The {@code mode} parameter is accepted for interface compatibility but is not used.
   *
   * @param durableClass the managed durable class
   * @param durable      the durable to cache; ignored when {@code null}
   * @param mode         not used by this implementation
   * @return the cached durable (existing or provided), or {@code null} when {@code durable} is {@code null}
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
   * Adds the supplied durable to a cached vector if that vector currently exists.
   *
   * @param vectorKey the cache key identifying the vector
   * @param durable   the durable to add; ignored when {@code null}
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
   * Removes the supplied durable from a cached vector, deleting the vector entirely when it is singular.
   *
   * @param vectorKey the cache key identifying the vector
   * @param durable   the durable to remove; ignored when {@code null}
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
   * Converts an arbitrary vector into the key-based intrinsic form required by this DAO,
   * leaving the vector unchanged when it is already the correct type.
   *
   * @param managedClass the durable class stored in the vector
   * @param vector       the vector to migrate
   * @return the original vector when already compatible, or a new key-based equivalent
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
   * Creates a new single-element vector that references the supplied durable by key.
   *
   * @param vectorKey         the cache key describing the target vector
   * @param durable           the durable to reference
   * @param timeToLiveSeconds the TTL for the vector in seconds
   * @return a new singular key-based vector
   */
  public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds) {

    return new ByKeySingularVector<>(new DurableKey<>(vectorKey.getElementClass(), durable.getId()), timeToLiveSeconds);
  }

  /**
   * Creates a new multi-element intrinsic vector from the provided durables.
   *
   * @param vectorKey         the cache key describing the target vector
   * @param elementIter       the durables to include
   * @param comparator        comparator for ordered vectors; {@code null} uses natural ordering
   * @param maxSize           maximum number of elements to retain; zero or negative means unbounded
   * @param timeToLiveSeconds the TTL for the vector in seconds
   * @param ordered           {@code true} to maintain elements in sorted order
   * @return a new {@link ByKeyIntrinsicVector}
   */
  public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    return new ByKeyIntrinsicVector<>(vectorKey.getElementClass(), elementIter, comparator, maxSize, timeToLiveSeconds, ordered);
  }
}
