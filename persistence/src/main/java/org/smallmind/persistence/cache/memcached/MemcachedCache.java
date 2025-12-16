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

import java.util.Arrays;
import java.util.Map;
import org.smallmind.memcached.utility.ProxyCASResponse;
import org.smallmind.memcached.utility.ProxyMemcachedClient;
import org.smallmind.persistence.cache.CASSupportingPersistenceCache;
import org.smallmind.persistence.cache.CASValue;
import org.smallmind.persistence.cache.CacheOperationException;

/**
 * {@link org.smallmind.persistence.cache.PersistenceCache} backed by a {@link ProxyMemcachedClient}.
 * Keys are namespaced with a discriminator to avoid collisions and optional CAS operations are supported.
 *
 * @param <V> value type stored in the cache
 */
public class MemcachedCache<V> implements CASSupportingPersistenceCache<String, V> {

  private final ProxyMemcachedClient memcachedClient;
  private final Class<V> valueClass;
  private final String discriminator;
  private final int timeToLiveSeconds;

  /**
   * Creates a memcached-backed cache instance.
   *
   * @param memcachedClient   client used to interact with the memcached cluster
   * @param discriminator     namespace applied to keys to avoid collisions
   * @param valueClass        class used to safely cast returned values
   * @param timeToLiveSeconds default time-to-live used when none is specified
   */
  public MemcachedCache (ProxyMemcachedClient memcachedClient, String discriminator, Class<V> valueClass, int timeToLiveSeconds) {

    this.valueClass = valueClass;
    this.memcachedClient = memcachedClient;
    this.discriminator = discriminator;
    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  /**
   * @return underlying memcached client
   */
  public ProxyMemcachedClient getMemcachedClient () {

    return memcachedClient;
  }

  /**
   * @return {@code false} because memcached CAS operations do not require copying values
   */
  @Override
  public boolean requiresCopyOnDistributedCASOperation () {

    return false;
  }

  /**
   * @return default TTL in seconds applied to cache entries
   */
  @Override
  public int getDefaultTimeToLiveSeconds () {

    return timeToLiveSeconds;
  }

  /**
   * Fetches a value from memcached.
   *
   * @param key cache key without discriminator prefix
   * @return cached value or {@code null} when missing
   * @throws CacheOperationException if the client interaction fails
   */
  @Override
  public V get (String key)
    throws CacheOperationException {

    try {

      return valueClass.cast(memcachedClient.get(getDiscriminatedKey(key)));
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  /**
   * Fetches multiple values from memcached.
   *
   * @param keys cache keys without discriminator prefix
   * @return map of keys to cached values; missing entries are absent
   * @throws CacheOperationException if the client interaction fails
   */
  @Override
  public Map<String, V> get (String[] keys)
    throws CacheOperationException {

    String[] discriminatedKeys = new String[keys.length];

    for (int index = 0; index < keys.length; index++) {
      discriminatedKeys[index] = getDiscriminatedKey(keys[index]);
    }

    try {

      return memcachedClient.get(Arrays.asList(discriminatedKeys));
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  /**
   * Stores a value with an optional custom TTL.
   *
   * @param key               cache key without discriminator prefix
   * @param value             value to cache
   * @param timeToLiveSeconds TTL in seconds; non-positive values fall back to the default TTL
   * @throws CacheOperationException if the client interaction fails
   */
  @Override
  public void set (String key, V value, int timeToLiveSeconds) {

    try {
      memcachedClient.set(getDiscriminatedKey(key), (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value);
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  /**
   * Stores a value only when no existing entry is found, retrying with CAS for safety.
   *
   * @param key               cache key without discriminator prefix
   * @param value             value to cache
   * @param timeToLiveSeconds TTL in seconds; non-positive values fall back to the default TTL
   * @return existing value when present; {@code null} when the new value was inserted
   * @throws CacheOperationException if the client interaction fails
   */
  @Override
  public V putIfAbsent (String key, V value, int timeToLiveSeconds) {

    try {

      ProxyCASResponse<V> getsResponse;
      String discriminatedKey = getDiscriminatedKey(key);

      if (((getsResponse = memcachedClient.casGet(discriminatedKey)) != null) && (getsResponse.getValue() != null)) {

        return getsResponse.getValue();
      }

      while (!memcachedClient.casSet(discriminatedKey, (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value, 0)) {
        if (((getsResponse = memcachedClient.casGet(discriminatedKey)) != null) && (getsResponse.getValue() != null)) {

          return getsResponse.getValue();
        }
      }

      return null;
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  /**
   * Retrieves a value and CAS token for optimistic updates.
   *
   * @param key cache key without discriminator prefix
   * @return CAS-wrapped value; {@link CASValue#nullInstance()} when missing
   * @throws CacheOperationException if the client interaction fails
   */
  @Override
  public CASValue<V> getViaCas (String key) {

    try {
      ProxyCASResponse<V> getsResponse;

      if ((getsResponse = memcachedClient.casGet(getDiscriminatedKey(key))) == null) {

        return CASValue.nullInstance();
      }

      return new CASValue<V>(getsResponse.getValue(), getsResponse.getCas());
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  /**
   * Attempts a CAS-based update of a cache entry.
   *
   * @param key               cache key without discriminator prefix
   * @param oldValue          ignored by the memcached client but included for interface parity
   * @param value             new value to store
   * @param version           CAS token obtained from {@link #getViaCas(String)}
   * @param timeToLiveSeconds TTL in seconds; non-positive values fall back to the default TTL
   * @return {@code true} when the CAS update succeeds
   * @throws CacheOperationException if the client interaction fails
   */
  @Override
  public boolean putViaCas (String key, V oldValue, V value, long version, int timeToLiveSeconds) {

    try {

      return memcachedClient.casSet(getDiscriminatedKey(key), (timeToLiveSeconds <= 0) ? getDefaultTimeToLiveSeconds() : timeToLiveSeconds, value, version);
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  /**
   * Deletes a cache entry.
   *
   * @param key cache key without discriminator prefix
   * @throws CacheOperationException if the client interaction fails
   */
  @Override
  public void remove (String key) {

    try {
      memcachedClient.delete(getDiscriminatedKey(key));
    } catch (Exception exception) {
      throw new CacheOperationException(exception);
    }
  }

  /**
   * Builds a fully qualified memcached key by applying the configured discriminator.
   *
   * @param key local cache key
   * @return discriminator-prefixed key used with the memcached client
   */
  private String getDiscriminatedKey (String key) {

    return new StringBuilder(discriminator).append('[').append(key).append(']').toString();
  }
}
