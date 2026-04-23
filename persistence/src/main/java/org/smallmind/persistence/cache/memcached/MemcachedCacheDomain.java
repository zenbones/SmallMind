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
package org.smallmind.persistence.cache.memcached;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.memcached.utility.ProxyMemcachedClient;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.EntitySource;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.PersistenceCache;

/**
 * {@link CacheDomain} implementation that provisions lazily created, memcached-backed
 * {@link MemcachedCache} instances for durable entities, wide entity lists, and durable vectors.
 *
 * <p>All caches within a domain share the same {@link ProxyMemcachedClient} and the same
 * discriminator namespace. A per-class TTL override map can be provided to vary the
 * time-to-live on a class-by-class basis; classes not present in the map receive the domain's
 * default TTL.</p>
 *
 * <p>Cache instances are created lazily on first use and are stored in {@link ConcurrentHashMap}s
 * with double-checked locking to guarantee safe single-instance creation in a concurrent
 * environment.</p>
 *
 * @param <I> the identifier type of the managed durable entities, must be {@link Serializable}
 *            and {@link Comparable}
 * @param <D> the durable entity type managed by this domain
 */
public class MemcachedCacheDomain<I extends Serializable & Comparable<I>, D extends Durable<I>> implements CacheDomain<I, D> {

  private final ProxyMemcachedClient memcachedClient;
  private final Map<Class<D>, Integer> timeTiLiveOverrideMap;
  private final ConcurrentHashMap<Class<D>, MemcachedCache<D>> instanceCacheMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Class<D>, MemcachedCache<List<D>>> wideInstanceCacheMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Class<D>, MemcachedCache<DurableVector<I, D>>> vectorCacheMap = new ConcurrentHashMap<>();
  private final String discriminator;
  private final int timeToLiveSeconds;

  /**
   * Constructs a cache domain with a uniform TTL applied to all managed entity classes.
   *
   * @param memcachedClient   the client used to interact with the memcached cluster
   * @param discriminator     the namespace prefix applied to every key in this domain
   * @param timeToLiveSeconds the default TTL in seconds for all cached entries
   */
  public MemcachedCacheDomain (ProxyMemcachedClient memcachedClient, String discriminator, int timeToLiveSeconds) {

    this(memcachedClient, discriminator, timeToLiveSeconds, null);
  }

  /**
   * Constructs a cache domain with an optional per-class TTL override map.
   *
   * @param memcachedClient       the client used to interact with the memcached cluster
   * @param discriminator         the namespace prefix applied to every key in this domain
   * @param timeToLiveSeconds     the default TTL in seconds for all cached entries
   * @param timeTiLiveOverrideMap optional map from managed entity class to a per-class TTL
   *                              override; may be {@code null}
   */
  public MemcachedCacheDomain (ProxyMemcachedClient memcachedClient, String discriminator, int timeToLiveSeconds, Map<Class<D>, Integer> timeTiLiveOverrideMap) {

    this.memcachedClient = memcachedClient;
    this.discriminator = discriminator;
    this.timeToLiveSeconds = timeToLiveSeconds;
    this.timeTiLiveOverrideMap = timeTiLiveOverrideMap;
  }

  /**
   * Returns the metric source identifier used when reporting cache statistics.
   *
   * @return the display name for {@link EntitySource#MEMCACHED}
   */
  @Override
  public String getMetricSource () {

    return EntitySource.MEMCACHED.getDisplay();
  }

  /**
   * Returns the {@link PersistenceCache} used to store and retrieve individual durable instances,
   * creating it lazily if necessary.
   *
   * @param managedClass the entity class for which the cache is required
   * @return the per-instance cache scoped to {@code managedClass}
   */
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

  /**
   * Returns the {@link PersistenceCache} used to store and retrieve lists of durable instances
   * (wide results), creating it lazily if necessary.
   *
   * @param managedClass the entity class for which the wide cache is required
   * @return the wide-instance cache scoped to {@code managedClass}
   */
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

  /**
   * Returns the {@link PersistenceCache} used to store and retrieve {@link DurableVector} entries,
   * creating it lazily if necessary.
   *
   * @param managedClass the entity class for which the vector cache is required
   * @return the vector cache scoped to {@code managedClass}
   */
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

  /**
   * Resolves the effective TTL for the given entity class by consulting the override map.
   *
   * <p>If {@code timeTiLiveOverrideMap} is non-null and contains an entry for
   * {@code managedClass}, that override is returned; otherwise the domain default is used.</p>
   *
   * @param managedClass the entity class whose TTL is being resolved
   * @return the TTL in seconds to use for entries cached on behalf of {@code managedClass}
   */
  private int getTimeToLiveSeconds (Class<D> managedClass) {

    Integer timeToLiveOverrideSeconds;

    if ((timeTiLiveOverrideMap != null) && ((timeToLiveOverrideSeconds = timeTiLiveOverrideMap.get(managedClass)) != null)) {

      return timeToLiveOverrideSeconds;
    }

    return timeToLiveSeconds;
  }
}
