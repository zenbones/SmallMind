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

import java.util.Map;

/**
 * Core cache abstraction used by the persistence cache DAOs, providing single and bulk get, set,
 * conditional put, and remove operations.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface PersistenceCache<K, V> {

  /**
   * Returns the TTL applied to entries when no explicit TTL is provided by the caller.
   *
   * @return default time-to-live in seconds
   */
  int getDefaultTimeToLiveSeconds ();

  /**
   * Returns the cached value for the given key.
   *
   * @param key cache key to look up
   * @return the cached value, or {@code null} if no entry exists
   * @throws CacheOperationException if the underlying cache operation fails
   */
  V get (K key)
    throws CacheOperationException;

  /**
   * Returns a map of cached values for all provided keys; keys with no entry are omitted.
   *
   * @param keys array of cache keys to fetch
   * @return map from key to cached value for every key that had an entry
   * @throws CacheOperationException if the underlying cache operation fails
   */
  Map<K, V> get (K[] keys)
    throws CacheOperationException;

  /**
   * Unconditionally stores a value under the given key with the specified TTL.
   *
   * @param key               cache key under which to store the value
   * @param value             value to store
   * @param timeToLiveSeconds TTL in seconds for this entry
   * @throws CacheOperationException if the underlying cache operation fails
   */
  void set (K key, V value, int timeToLiveSeconds)
    throws CacheOperationException;

  /**
   * Stores a value only if no entry currently exists for the key.
   *
   * @param key               cache key to store the value under
   * @param value             value to store if absent
   * @param timeToLiveSeconds TTL in seconds for the new entry
   * @return the pre-existing value if an entry was already present, otherwise {@code null}
   * @throws CacheOperationException if the underlying cache operation fails
   */
  V putIfAbsent (K key, V value, int timeToLiveSeconds)
    throws CacheOperationException;

  /**
   * Evicts the entry for the given key from the cache.
   *
   * @param key cache key whose entry should be removed
   * @throws CacheOperationException if the underlying cache operation fails
   */
  void remove (K key)
    throws CacheOperationException;
}
