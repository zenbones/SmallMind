/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.persistence.cache.memcached;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.memcached.ProxyMemcachedClient;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.EntitySource;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.PersistenceCache;

public class MemcachedCacheDomain<I extends Serializable & Comparable<I>, D extends Durable<I>> implements CacheDomain<I, D> {

  private final ProxyMemcachedClient memcachedClient;
  private final Map<Class<D>, Integer> timeTiLiveOverrideMap;
  private final ConcurrentHashMap<Class<D>, MemcachedCache<D>> instanceCacheMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Class<D>, MemcachedCache<List<D>>> wideInstanceCacheMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Class<D>, MemcachedCache<DurableVector<I, D>>> vectorCacheMap = new ConcurrentHashMap<>();
  private final String discriminator;
  private final int timeToLiveSeconds;

  public MemcachedCacheDomain (ProxyMemcachedClient memcachedClient, String discriminator, int timeToLiveSeconds) {

    this(memcachedClient, discriminator, timeToLiveSeconds, null);
  }

  public MemcachedCacheDomain (ProxyMemcachedClient memcachedClient, String discriminator, int timeToLiveSeconds, Map<Class<D>, Integer> timeTiLiveOverrideMap) {

    this.memcachedClient = memcachedClient;
    this.discriminator = discriminator;
    this.timeToLiveSeconds = timeToLiveSeconds;
    this.timeTiLiveOverrideMap = timeTiLiveOverrideMap;
  }

  @Override
  public String getMetricSource () {

    return EntitySource.MEMCACHED.getDisplay();
  }

  @Override
  public PersistenceCache<String, D> getInstanceCache (Class<D> managedClass) {

    MemcachedCache<D> instanceCache;

    if ((instanceCache = instanceCacheMap.get(managedClass)) == null) {
      synchronized (instanceCacheMap) {
        if ((instanceCache = instanceCacheMap.get(managedClass)) == null) {
          instanceCacheMap.put(managedClass, instanceCache = new MemcachedCache<>(memcachedClient, discriminator, managedClass, getTimeToLiveSeconds(managedClass)));
        }
      }
    }

    return instanceCache;
  }

  @Override
  public PersistenceCache<String, List<D>> getWideInstanceCache (Class<D> managedClass) {

    MemcachedCache<List<D>> wideInstanceCache;

    if ((wideInstanceCache = wideInstanceCacheMap.get(managedClass)) == null) {
      synchronized (wideInstanceCacheMap) {
        if ((wideInstanceCache = wideInstanceCacheMap.get(managedClass)) == null) {
          wideInstanceCacheMap.put(managedClass, wideInstanceCache = new MemcachedCache(memcachedClient, discriminator, List.class, getTimeToLiveSeconds(managedClass)));
        }
      }
    }

    return wideInstanceCache;
  }

  @Override
  public PersistenceCache<String, DurableVector<I, D>> getVectorCache (Class<D> managedClass) {

    MemcachedCache<DurableVector<I, D>> vectorCache;

    if ((vectorCache = vectorCacheMap.get(managedClass)) == null) {
      synchronized (vectorCacheMap) {
        if ((vectorCache = vectorCacheMap.get(managedClass)) == null) {
          vectorCacheMap.put(managedClass, vectorCache = new MemcachedCache(memcachedClient, discriminator, DurableVector.class, getTimeToLiveSeconds(managedClass)));
        }
      }
    }

    return vectorCache;
  }

  private int getTimeToLiveSeconds (Class<D> managedClass) {

    Integer timeToLiveOverrideSeconds;

    if ((timeTiLiveOverrideMap != null) && ((timeToLiveOverrideSeconds = timeTiLiveOverrideMap.get(managedClass)) != null)) {

      return timeToLiveOverrideSeconds;
    }

    return timeToLiveSeconds;
  }
}
