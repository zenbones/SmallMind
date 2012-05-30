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
package org.smallmind.persistence.cache.memcached;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.rubyeye.xmemcached.MemcachedClient;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.PersistenceCache;

public class MemcachedCacheDomain<I extends Comparable<I>, D extends Durable<I>> implements CacheDomain<I, D> {

  private final MemcachedClient memcachedClient;
  private final Map<Class<D>, Integer> timeTiLiveOverrideMap;
  private final ConcurrentHashMap<Class<D>, MemcachedCache<D>> instanceCacheMap = new ConcurrentHashMap<Class<D>, MemcachedCache<D>>();
  private final ConcurrentHashMap<Class<D>, MemcachedCache<DurableVector>> vectorCacheMap = new ConcurrentHashMap<Class<D>, MemcachedCache<DurableVector>>();
  private final String discriminator;
  private final int timeToLiveSeconds;

  public MemcachedCacheDomain (MemcachedClient memcachedClient, String discriminator, int timeToLiveSeconds) {

    this(memcachedClient, discriminator, timeToLiveSeconds, null);
  }

  public MemcachedCacheDomain (MemcachedClient memcachedClient, String discriminator, int timeToLiveSeconds, Map<Class<D>, Integer> timeTiLiveOverrideMap) {

    this.memcachedClient = memcachedClient;
    this.discriminator = discriminator;
    this.timeToLiveSeconds = timeToLiveSeconds;
    this.timeTiLiveOverrideMap = timeTiLiveOverrideMap;
  }

  @Override
  public String getStatisticsSource () {

    return "memcached";
  }

  @Override
  public PersistenceCache<String, D> getInstanceCache (Class<D> managedClass) {

    MemcachedCache<D> instanceCache;

    if ((instanceCache = instanceCacheMap.get(managedClass)) == null) {
      synchronized (instanceCacheMap) {
        if ((instanceCache = instanceCacheMap.get(managedClass)) == null) {
          instanceCacheMap.put(managedClass, instanceCache = new MemcachedCache<D>(memcachedClient, discriminator, managedClass, getTimeToLiveSeconds(managedClass)));
        }
      }
    }

    return instanceCache;
  }

  @Override
  public PersistenceCache<String, DurableVector> getVectorCache (Class<D> managedClass) {

    MemcachedCache<DurableVector> vectorCache;

    if ((vectorCache = vectorCacheMap.get(managedClass)) == null) {
      synchronized (vectorCacheMap) {
        if ((vectorCache = vectorCacheMap.get(managedClass)) == null) {
          vectorCacheMap.put(managedClass, vectorCache = new MemcachedCache<DurableVector>(memcachedClient, discriminator, DurableVector.class, getTimeToLiveSeconds(managedClass)));
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
