/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.smallmind.persistence.Durable;

public abstract class AbstractCacheDao<I extends Comparable<I>, D extends Durable<I>> implements CacheDao<I, D> {

  private CacheDomain<I, D> cacheDomain;

  public AbstractCacheDao (CacheDomain<I, D> cacheDomain) {

    this.cacheDomain = cacheDomain;
  }

  public String getMetricSource () {

    return cacheDomain.getMetricSource();
  }

  public PersistenceCache<String, D> getInstanceCache (Class<D> durableClass) {

    return cacheDomain.getInstanceCache(durableClass);
  }

  public PersistenceCache<String, DurableVector> getVectorCache (Class<D> durableClass) {

    return cacheDomain.getVectorCache(durableClass);
  }

  public D get (Class<D> durableClass, I id) {

    return acquire(durableClass, id);
  }

  public Map<DurableKey<I, D>, D> get (Class<D> durableClass, List<DurableKey<I, D>> durableKeys) {

    Map<DurableKey<I, D>, D> resultMap = new HashMap<DurableKey<I, D>, D>();
    HashMap<String, DurableKey<I, D>> durableKeyMap = new HashMap<String, DurableKey<I, D>>();
    String[] keys = new String[durableKeys.size()];
    int index = 0;

    for (DurableKey<I, D> durableKey : durableKeys) {
      keys[index] = durableKey.getKey();
      durableKeyMap.put(keys[index++], durableKey);
    }

    for (Map.Entry<String, D> resultEntry : getInstanceCache(durableClass).get(keys).entrySet()) {
      resultMap.put(durableKeyMap.get(resultEntry.getKey()), resultEntry.getValue());
    }

    return resultMap;
  }

  public D acquire (Class<D> durableClass, I id) {

    DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, id);

    return getInstanceCache(durableClass).get(durableKey.getKey());
  }

  public void delete (Class<D> durableClass, D durable) {

    if (durable != null) {

      DurableKey<I, D> durableKey = new DurableKey<I, D>(durableClass, durable.getId());

      getInstanceCache(durableClass).remove(durableKey.getKey());
    }
  }

  public DurableVector<I, D> getVector (VectorKey<D> vectorKey) {

    return getVectorCache(vectorKey.getElementClass()).get(vectorKey.getKey());
  }

  public DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector) {

    DurableVector<I, D> migratedVector;
    DurableVector<I, D> cachedVector;

    migratedVector = migrateVector(vectorKey.getElementClass(), vector);

    return ((cachedVector = getVectorCache(vectorKey.getElementClass()).putIfAbsent(vectorKey.getKey(), migratedVector, migratedVector.getTimeToLiveSeconds())) != null) ? cachedVector : vector;
  }

  public void deleteVector (VectorKey<D> vectorKey) {

    getVectorCache(vectorKey.getElementClass()).remove(vectorKey.getKey());
  }
}