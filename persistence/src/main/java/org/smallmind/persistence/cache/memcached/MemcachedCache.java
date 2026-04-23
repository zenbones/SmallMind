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
 * {@link CASSupportingPersistenceCache} implementation backed by a memcached cluster via a
 * {@link ProxyMemcachedClient}.
 *
 * <p>Every cache key is namespaced with a configurable discriminator string to prevent collisions
 * when multiple cache domains share the same memcached cluster. The namespaced key form is
 * {@code discriminator[key]}.</p>
 *
 * <p>CAS (compare-and-swap) operations are supported through the {@link #getViaCas(String)} and
 * {@link #putViaCas(String, Object, Object, long, int)} methods, allowing optimistic-locking
 * patterns. The {@link #putIfAbsent(String, Object, int)} method uses a CAS loop to guarantee
 * atomic insert-if-absent semantics.</p>
 *
 * <p>Any exception thrown by the underlying client is wrapped in a {@link CacheOperationException}
 * so that callers receive a uniform exception type regardless of the client implementation.</p>
 *
 * @param <V> the type of value stored in this cache
 */
public class MemcachedCache<V> implements CASSupportingPersistenceCache<String, V> {

  private final ProxyMemcachedClient memcachedClient;
  private final Class<V> valueClass;
  private final String discriminator;
  private final int timeToLiveSeconds;

  /**
   * Constructs a memcached-backed cache with the given parameters.
   *
   * @param memcachedClient   the client used to interact with the memcached cluster
   * @param discriminator     the namespace prefix applied to all keys managed by this cache
   * @param valueClass        the {@link Class} token used to safely cast retrieved objects
   * @param timeToLiveSeconds the default TTL in seconds applied when no TTL override is provided
   */
  public MemcachedCache (ProxyMemcachedClient memcachedClient, String discriminator, Class<V> valueClass, int timeToLiveSeconds) {

    this.valueClass = valueClass;
    this.memcachedClient = memcachedClient;
    this.discriminator = discriminator;
    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  /**
   * Returns the underlying {@link ProxyMemcachedClient} used by this cache.
   *
   * @return the memcached client
   */
  public ProxyMemcachedClient getMemcachedClient () {

    return memcachedClient;
  }

  /**
   * Reports that this cache does not require value copying before performing a distributed CAS
   * update, because memcached serialises values independently of the application heap.
   *
   * @return {@code false}
   */
  @Override
  public boolean requiresCopyOnDistributedCASOperation () {

    return false;
  }

  /**
   * Returns the default time-to-live applied to entries stored without an explicit TTL override.
   *
   * @return the default TTL in seconds
   */
  @Override
  public int getDefaultTimeToLiveSeconds () {

    return timeToLiveSeconds;
  }

  /**
   * Retrieves a single value from memcached by its discriminated key.
   *
   * @param key the local (non-discriminated) cache key
   * @return the cached value, or {@code null} if absent or expired
   * @throws CacheOperationException if the underlying client call fails
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
   * Retrieves multiple values from memcached in a single bulk request.
   *
   * <p>The supplied keys are each discriminated before being sent to the client. Absent or
   * expired keys are omitted from the result map.</p>
   *
   * @param keys the local (non-discriminated) cache keys to look up
   * @return a map of local keys to their cached values
   * @throws CacheOperationException if the underlying client call fails
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
   * Stores a value in memcached, overwriting any existing entry.
   *
   * <p>If {@code timeToLiveSeconds} is non-positive the configured default TTL is used instead.</p>
   *
   * @param key               the local (non-discriminated) cache key
   * @param value             the value to cache
   * @param timeToLiveSeconds the TTL in seconds; non-positive values fall back to the default
   * @throws CacheOperationException if the underlying client call fails
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
   * Inserts a value only when the key is not already populated, using a CAS loop for safety.
   *
   * <p>If an existing, non-null value is found it is returned immediately. Otherwise a CAS-based
   * insert is attempted in a loop until it succeeds or a concurrent insert is detected, at which
   * point the concurrently inserted value is returned.</p>
   *
   * @param key               the local (non-discriminated) cache key
   * @param value             the value to insert if absent
   * @param timeToLiveSeconds the TTL in seconds; non-positive values fall back to the default
   * @return the existing value if the key was already populated; {@code null} if the new value
   * was successfully inserted
   * @throws CacheOperationException if the underlying client calls fail
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
   * Retrieves a value together with the CAS token required for a subsequent optimistic update.
   *
   * @param key the local (non-discriminated) cache key
   * @return a {@link CASValue} wrapping the value and its token; {@link CASValue#nullInstance()}
   * when the key is absent or expired
   * @throws CacheOperationException if the underlying client call fails
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
   * Attempts to replace the current cached value using CAS semantics.
   *
   * <p>The update succeeds only when the server's current CAS token for the key matches
   * {@code version}. If another thread or process has modified the entry since {@code version}
   * was obtained the operation fails and the caller should retry.</p>
   *
   * @param key               the local (non-discriminated) cache key
   * @param oldValue          the previous value (included for interface parity; not sent to the server)
   * @param value             the new value to store
   * @param version           the CAS token obtained from a prior {@link #getViaCas(String)} call
   * @param timeToLiveSeconds the TTL in seconds; non-positive values fall back to the default
   * @return {@code true} if the CAS update was applied; {@code false} on a version mismatch
   * @throws CacheOperationException if the underlying client call fails
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
   * Removes the entry for the given key from memcached.
   *
   * @param key the local (non-discriminated) cache key to delete
   * @throws CacheOperationException if the underlying client call fails
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
   * Builds the fully qualified memcached key by prepending the discriminator namespace.
   *
   * <p>The resulting format is {@code discriminator[key]}.</p>
   *
   * @param key the local cache key
   * @return the discriminator-prefixed key used with the memcached client
   */
  private String getDiscriminatedKey (String key) {

    return new StringBuilder(discriminator).append('[').append(key).append(']').toString();
  }
}
