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
package org.smallmind.persistence.cache.praxis.extrinsic;

import java.io.Serializable;
import java.util.Comparator;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.AbstractCacheDao;
import org.smallmind.persistence.cache.CASSupportingPersistenceCache;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.praxis.ByKeySingularVector;

/**
 * Cache DAO for extrinsic (out-of-process) caches that require compare-and-swap (CAS) semantics.
 * Vectors are stored as key-based structures to minimize serialized size across process boundaries.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByKeyExtrinsicCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  /**
   * Creates a DAO backed by the provided cache domain.
   *
   * @param cacheDomain the cache domain supplying instance and vector caches
   */
  public ByKeyExtrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

  /**
   * Stores a durable in the instance cache according to the requested update mode.
   * In {@code SOFT} mode an existing cached value takes precedence; in {@code HARD} mode the value is always overwritten.
   *
   * @param durableClass the managed durable class
   * @param durable      the durable to cache; ignored when {@code null}
   * @param mode         controls whether to overwrite an existing cache entry
   * @return the cached durable (existing or provided), or {@code null} when {@code durable} is {@code null}
   * @throws UnknownSwitchCaseException when an unrecognized {@link UpdateMode} is encountered
   */
  public D persist (Class<D> durableClass, D durable, UpdateMode mode) {

    if (durable != null) {

      D cachedDurable;
      DurableKey<I, D> durableKey = new DurableKey<>(durableClass, durable.getId());

      switch (mode) {
        case SOFT:

          return ((cachedDurable = getInstanceCache(durableClass).putIfAbsent(durableKey.getKey(), durable, 0)) != null) ? cachedDurable : durable;
        case HARD:
          getInstanceCache(durableClass).set(durableKey.getKey(), durable, 0);

          return durable;
        default:
          throw new UnknownSwitchCaseException(mode.name());
      }
    }

    return null;
  }

  /**
   * Adds or updates a durable within a cached vector using optimistic CAS operations.
   * The loop retries until the CAS succeeds or the vector no longer exists.
   *
   * @param vectorKey the cache key identifying the vector
   * @param durable   the durable to insert or update; ignored when {@code null}
   */
  public void updateInVector (VectorKey<D> vectorKey, D durable) {

    if (durable != null) {

      CASSupportingPersistenceCache<String, DurableVector<I, D>> persistenceCache = (CASSupportingPersistenceCache<String, DurableVector<I, D>>)getVectorCache(vectorKey.getElementClass());
      CASValue<DurableVector<I, D>> casValue;
      DurableVector<I, D> vectorCopy;

      do {
        if ((casValue = persistenceCache.getViaCas(vectorKey.getKey())).getValue() == null) {
          break;
        }

        vectorCopy = (!persistenceCache.requiresCopyOnDistributedCASOperation()) ? null : casValue.getValue().copy();
        if (!casValue.getValue().add(durable)) {
          break;
        }
      } while (!persistenceCache.putViaCas(vectorKey.getKey(), vectorCopy, casValue.getValue(), casValue.getVersion(), casValue.getValue().getTimeToLiveSeconds()));
    }
  }

  /**
   * Removes a durable from a cached vector using optimistic CAS operations, deleting the vector entirely
   * when it is singular.
   *
   * @param vectorKey the cache key identifying the vector
   * @param durable   the durable to remove; ignored when {@code null}
   */
  public void removeFromVector (VectorKey<D> vectorKey, D durable) {

    if (durable != null) {

      CASSupportingPersistenceCache<String, DurableVector<I, D>> persistenceCache = (CASSupportingPersistenceCache<String, DurableVector<I, D>>)getVectorCache(vectorKey.getElementClass());
      CASValue<DurableVector<I, D>> casValue;
      DurableVector<I, D> vectorCopy;

      do {
        if ((casValue = persistenceCache.getViaCas(vectorKey.getKey())).getValue() == null) {
          break;
        } else if (casValue.getValue().isSingular()) {
          deleteVector(vectorKey);
          break;
        }

        vectorCopy = (!persistenceCache.requiresCopyOnDistributedCASOperation()) ? null : casValue.getValue().copy();
        if (!casValue.getValue().remove(durable)) {
          break;
        }
      } while (!persistenceCache.putViaCas(vectorKey.getKey(), vectorCopy, casValue.getValue(), casValue.getVersion(), casValue.getValue().getTimeToLiveSeconds()));
    }
  }

  /**
   * Converts an arbitrary vector implementation into the key-based extrinsic form required by this DAO,
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
      if (!(vector instanceof ByKeyExtrinsicVector)) {

        return new ByKeyExtrinsicVector<>(managedClass, vector.asBestEffortPreFetchedList(), vector.getComparator(), vector.getMaxSize(), vector.getTimeToLiveSeconds(), vector.isOrdered());
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
   * Creates a new multi-element extrinsic vector from the provided durables.
   *
   * @param vectorKey         the cache key describing the target vector
   * @param elementIter       the durables to include
   * @param comparator        comparator for ordered vectors; {@code null} uses natural ordering
   * @param maxSize           maximum number of elements to retain; zero or negative means unbounded
   * @param timeToLiveSeconds the TTL for the vector in seconds
   * @param ordered           {@code true} to maintain elements in sorted order
   * @return a new {@link ByKeyExtrinsicVector}
   */
  public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    return new ByKeyExtrinsicVector<>(vectorKey.getElementClass(), elementIter, comparator, maxSize, timeToLiveSeconds, ordered);
  }
}
