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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.smallmind.persistence.Durable;

/**
 * Convenience base implementation of {@link CacheDao} that performs common cache lookups and
 * deletions for durables and vectors.
 */
public abstract class AbstractCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> implements CacheDao<I, D> {

  private final CacheDomain<I, D> cacheDomain;

  /**
   * @param cacheDomain grouping of caches for a particular durable type
   */
  public AbstractCacheDao (CacheDomain<I, D> cacheDomain) {

    this.cacheDomain = cacheDomain;
  }

  /**
   * @return identifier used when emitting cache metrics
   */
  public String getMetricSource () {

    return cacheDomain.getMetricSource();
  }

  /**
   * Provides the instance cache for the managed durable class.
   *
   * @param durableClass durable type
   * @return persistence cache keyed by durable id
   */
  public PersistenceCache<String, D> getInstanceCache (Class<D> durableClass) {

    return cacheDomain.getInstanceCache(durableClass);
  }

  /**
   * Provides the vector cache for the managed durable class.
   *
   * @param durableClass durable type
   * @return persistence cache storing durable vectors
   */
  public PersistenceCache<String, DurableVector<I, D>> getVectorCache (Class<D> durableClass) {

    return cacheDomain.getVectorCache(durableClass);
  }

  /**
   * Retrieves a durable from the instance cache by id.
   *
   * @param durableClass durable class
   * @param id           durable id
   * @return cached durable or {@code null}
   */
  public D get (Class<D> durableClass, I id) {

    DurableKey<I, D> durableKey = new DurableKey<>(durableClass, id);

    return getInstanceCache(durableClass).get(durableKey.getKey());
  }

  /**
   * Bulk fetch of durables from the instance cache.
   *
   * @param durableClass durable type
   * @param durableKeys  keys representing requested durables
   * @return map of keys to cached durables; missing entries are omitted
   */
  public Map<DurableKey<I, D>, D> get (Class<D> durableClass, List<DurableKey<I, D>> durableKeys) {

    Map<DurableKey<I, D>, D> resultMap = new HashMap<>();

    if ((durableKeys != null) && (!durableKeys.isEmpty())) {

      HashMap<String, DurableKey<I, D>> durableKeyMap = new HashMap<>();
      Map<String, D> valueMap;
      String[] keys = new String[durableKeys.size()];
      int index = 0;

      for (DurableKey<I, D> durableKey : durableKeys) {
        keys[index] = durableKey.getKey();
        durableKeyMap.put(keys[index++], durableKey);
      }

      if ((valueMap = getInstanceCache(durableClass).get(keys)) != null) {
        for (Map.Entry<String, D> resultEntry : valueMap.entrySet()) {
          resultMap.put(durableKeyMap.get(resultEntry.getKey()), resultEntry.getValue());
        }
      }
    }

    return resultMap;
  }

  /**
   * Removes a durable entry from the cache.
   *
   * @param durableClass durable type
   * @param durable      durable instance to delete
   */
  public void delete (Class<D> durableClass, D durable) {

    if (durable != null) {

      DurableKey<I, D> durableKey = new DurableKey<>(durableClass, durable.getId());

      getInstanceCache(durableClass).remove(durableKey.getKey());
    }
  }

  /**
   * Retrieves a durable vector from the cache.
   *
   * @param vectorKey key identifying the vector
   * @return cached vector or {@code null} when missing
   */
  public DurableVector<I, D> getVector (VectorKey<D> vectorKey) {

    return getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey());
  }

  /**
   * Persists a vector into the cache, migrating it to the expected implementation when needed.
   *
   * @param vectorKey key identifying the vector
   * @param vector    vector to store
   * @return existing cached vector when one was present, otherwise the supplied vector
   */
  public DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector) {

    DurableVector<I, D> migratedVector;
    DurableVector<I, D> cachedVector;

    migratedVector = migrateVector(vectorKey.getElementClass(), vector);

    return ((cachedVector = getVectorCache(vectorKey.getElementClass()).putIfAbsent(vectorKey.getKey(), migratedVector, migratedVector.getTimeToLiveSeconds())) != null) ? cachedVector : vector;
  }

  /**
   * Deletes a cached vector.
   *
   * @param vectorKey key identifying the vector to remove
   */
  public void deleteVector (VectorKey<D> vectorKey) {

    getVectorCache(vectorKey.getElementClass()).remove(vectorKey.getKey());
  }
}
