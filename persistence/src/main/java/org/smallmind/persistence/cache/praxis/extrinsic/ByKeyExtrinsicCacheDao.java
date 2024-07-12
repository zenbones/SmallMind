/*
 * Copyright (c) 2007 through 2024 David Berkman
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

// The cache is external to the JVM, or lacks thread-safe operations, and requires CAS operations
// The vector cache references the instance cache by a unique key
public class ByKeyExtrinsicCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractCacheDao<I, D> {

  public ByKeyExtrinsicCacheDao (CacheDomain<I, D> cacheDomain) {

    super(cacheDomain);
  }

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

  public DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds) {

    return new ByKeySingularVector<>(new DurableKey<>(vectorKey.getElementClass(), durable.getId()), timeToLiveSeconds);
  }

  public DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    return new ByKeyExtrinsicVector<>(vectorKey.getElementClass(), elementIter, comparator, maxSize, timeToLiveSeconds, ordered);
  }
}
