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
 * Skeletal implementation of {@link CacheDao} that delegates instance and vector cache access to a
 * {@link CacheDomain}, providing shared get, delete, and vector persistence logic.
 *
 * @param <I> durable identifier type
 * @param <D> durable type
 */
public abstract class AbstractCacheDao<I extends Serializable & Comparable<I>, D extends Durable<I>> implements CacheDao<I, D> {

  private final CacheDomain<I, D> cacheDomain;

  /**
   * Constructs a cache DAO backed by the given cache domain.
   *
   * @param cacheDomain cache domain providing instance and vector caches for the durable type
   */
  public AbstractCacheDao (CacheDomain<I, D> cacheDomain) {

    this.cacheDomain = cacheDomain;
  }

  /**
   * Returns the metric source identifier supplied by the underlying cache domain.
   *
   * @return string used to tag cache metrics
   */
  public String getMetricSource () {

    return cacheDomain.getMetricSource();
  }

  /**
   * Returns the instance cache for the given durable type.
   *
   * @param durableClass durable class whose instance cache is required
   * @return persistence cache keyed by durable id string
   */
  public PersistenceCache<String, D> getInstanceCache (Class<D> durableClass) {

    return cacheDomain.getInstanceCache(durableClass);
  }

  /**
   * Returns the vector cache for the given durable type.
   *
   * @param durableClass durable class whose vector cache is required
   * @return persistence cache storing {@link DurableVector} instances keyed by vector key string
   */
  public PersistenceCache<String, DurableVector<I, D>> getVectorCache (Class<D> durableClass) {

    return cacheDomain.getVectorCache(durableClass);
  }

  /**
   * Looks up a single durable in the instance cache by its identifier.
   *
   * @param durableClass durable class to look up
   * @param id           identifier of the durable
   * @return the cached durable, or {@code null} if not present
   */
  public D get (Class<D> durableClass, I id) {

    DurableKey<I, D> durableKey = new DurableKey<>(durableClass, id);

    return getInstanceCache(durableClass).get(durableKey.getKey());
  }

  /**
   * Fetches multiple durables from the instance cache in a single call.
   *
   * @param durableClass durable class for all requested keys
   * @param durableKeys  list of keys identifying the durables to retrieve
   * @return map of {@link DurableKey} to cached durable; keys with no cache entry are absent
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
   * Removes the given durable from the instance cache.
   *
   * @param durableClass durable class whose cache entry is to be evicted
   * @param durable      durable instance to remove; no-op when {@code null}
   */
  public void delete (Class<D> durableClass, D durable) {

    if (durable != null) {

      DurableKey<I, D> durableKey = new DurableKey<>(durableClass, durable.getId());

      getInstanceCache(durableClass).remove(durableKey.getKey());
    }
  }

  /**
   * Looks up a cached vector by its key.
   *
   * @param vectorKey key identifying the vector to retrieve
   * @return the cached vector, or {@code null} if absent
   */
  public DurableVector<I, D> getVector (VectorKey<D> vectorKey) {

    return getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey());
  }

  /**
   * Stores a vector in the cache if no entry exists for the key, after migrating it to the correct
   * concrete type.
   *
   * @param vectorKey key under which the vector should be stored
   * @param vector    vector to migrate and store
   * @return the pre-existing cached vector if one was present, otherwise the newly stored vector
   */
  public DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector) {

    DurableVector<I, D> migratedVector;
    DurableVector<I, D> cachedVector;

    migratedVector = migrateVector(vectorKey.getElementClass(), vector);

    return ((cachedVector = getVectorCache(vectorKey.getElementClass()).putIfAbsent(vectorKey.getKey(), migratedVector, migratedVector.getTimeToLiveSeconds())) != null) ? cachedVector : vector;
  }

  /**
   * Removes the vector identified by the given key from the vector cache.
   *
   * @param vectorKey key of the vector to evict
   */
  public void deleteVector (VectorKey<D> vectorKey) {

    getVectorCache(vectorKey.getElementClass()).remove(vectorKey.getKey());
  }
}
