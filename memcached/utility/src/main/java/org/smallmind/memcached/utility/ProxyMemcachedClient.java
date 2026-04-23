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
package org.smallmind.memcached.utility;

import java.util.Collection;
import java.util.Map;

/**
 * Common abstraction over memcached client implementations used throughout the SmallMind persistence
 * and caching layers.
 *
 * <p>This interface normalises the surface area exposed by real memcached clients (such as the
 * Cubby NIO client) and the {@link InMemoryMemcachedClient} used in tests, allowing higher-level
 * components to be written against a single, stable API. Every operation that interacts with the
 * cache server declares {@code throws Exception} so that implementations can propagate
 * client-specific checked exceptions without requiring this interface to depend on any particular
 * client library.</p>
 *
 * <p>CAS (compare-and-swap) variants are provided alongside their unconditional counterparts to
 * support optimistic-locking patterns.</p>
 */
public interface ProxyMemcachedClient {

  /**
   * Returns the default timeout applied to cache operations when the caller does not specify one.
   *
   * @return the default request timeout in milliseconds
   */
  long getDefaultTimeout ();

  /**
   * Wraps a value and its associated CAS token in an implementation-specific {@link ProxyCASResponse}.
   *
   * @param cas   the compare-and-swap token to associate with the value
   * @param value the cached value to wrap
   * @param <T>   the value type
   * @return a {@link ProxyCASResponse} holding both the value and the token
   */
  <T> ProxyCASResponse<T> createCASResponse (long cas, T value);

  /**
   * Retrieves the cached value for the given key.
   *
   * @param key the cache key
   * @param <T> the expected value type
   * @return the cached value, or {@code null} if the key is absent or expired
   * @throws Exception if the cache operation fails
   */
  <T> T get (String key)
    throws Exception;

  /**
   * Retrieves cached values for a collection of keys in a single bulk operation.
   *
   * @param keys the cache keys to look up
   * @param <T>  the expected value type
   * @return a map of keys to their cached values; absent or expired keys are omitted
   * @throws Exception if the cache operation fails
   */
  <T> Map<String, T> get (Collection<String> keys)
    throws Exception;

  /**
   * Retrieves the cached value for the given key together with its CAS token.
   *
   * @param key the cache key
   * @param <T> the expected value type
   * @return a {@link ProxyCASResponse} containing the value and its token, or {@code null} when
   * the key is absent or expired
   * @throws Exception if the cache operation fails
   */
  <T> ProxyCASResponse<T> casGet (String key)
    throws Exception;

  /**
   * Stores a value unconditionally, replacing any existing entry.
   *
   * @param key        the cache key
   * @param expiration the time-to-live in seconds; {@code 0} means no expiration
   * @param value      the value to store
   * @param <T>        the value type
   * @return {@code true} if the value was stored successfully
   * @throws Exception if the cache operation fails
   */
  <T> boolean set (String key, int expiration, T value)
    throws Exception;

  /**
   * Stores a value only if the supplied CAS token still matches the server's current token for
   * the key, providing an optimistic-lock write.
   *
   * @param key        the cache key
   * @param expiration the time-to-live in seconds; {@code 0} means no expiration
   * @param value      the new value to store
   * @param cas        the CAS token obtained from a prior {@link #casGet(String)} call
   * @param <T>        the value type
   * @return {@code true} if the value was stored; {@code false} on a CAS mismatch
   * @throws Exception if the cache operation fails
   */
  <T> boolean casSet (String key, int expiration, T value, long cas)
    throws Exception;

  /**
   * Removes the entry for the given key unconditionally.
   *
   * @param key the cache key to delete
   * @return {@code true} if the entry was deleted or did not exist
   * @throws Exception if the cache operation fails
   */
  boolean delete (String key)
    throws Exception;

  /**
   * Removes the entry for the given key only if the supplied CAS token matches.
   *
   * @param key the cache key to delete
   * @param cas the CAS token that must match the stored entry
   * @return {@code true} if the entry was deleted or was already absent/expired; {@code false}
   * on a CAS mismatch
   * @throws Exception if the cache operation fails
   */
  boolean casDelete (String key, long cas)
    throws Exception;

  /**
   * Updates the expiration of an existing cache entry without returning its value.
   *
   * @param key        the cache key whose expiration should be refreshed
   * @param expiration the new time-to-live in seconds
   * @return {@code true} if the entry existed and was touched; {@code false} if absent or expired
   * @throws Exception if the cache operation fails
   */
  boolean touch (String key, int expiration)
    throws Exception;

  /**
   * Retrieves the cached value and atomically updates its expiration.
   *
   * @param key        the cache key
   * @param expiration the new time-to-live in seconds
   * @param <T>        the expected value type
   * @return the cached value, or {@code null} if the key is absent or expired
   * @throws Exception if the cache operation fails
   */
  <T> T getAndTouch (String key, int expiration)
    throws Exception;

  /**
   * Removes all entries from the cache.
   *
   * @throws Exception if the cache operation fails
   */
  void clear ()
    throws Exception;

  /**
   * Shuts down the client and releases all associated resources.
   *
   * @throws Exception if an error occurs during shutdown
   */
  void shutdown ()
    throws Exception;
}
