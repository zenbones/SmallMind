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
 * Abstraction over a persistence cache used by the cache DAOs.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface PersistenceCache<K, V> {

  /**
   * @return default TTL in seconds applied when none is specified
   */
  int getDefaultTimeToLiveSeconds ();

  /**
   * Retrieves a value by key.
   *
   * @param key cache key
   * @return cached value or {@code null}
   * @throws CacheOperationException on cache access failure
   */
  V get (K key)
    throws CacheOperationException;

  /**
   * Retrieves a map of values for the provided keys.
   *
   * @param keys keys to fetch
   * @return map of found entries (may be empty)
   * @throws CacheOperationException on cache access failure
   */
  Map<K, V> get (K[] keys)
    throws CacheOperationException;

  /**
   * Unconditionally sets a value with the specified TTL.
   *
   * @param key               cache key
   * @param value             value to store
   * @param timeToLiveSeconds TTL in seconds
   * @throws CacheOperationException on cache write failure
   */
  void set (K key, V value, int timeToLiveSeconds)
    throws CacheOperationException;

  /**
   * Stores the value only if no entry exists.
   *
   * @param key               cache key
   * @param value             value to store
   * @param timeToLiveSeconds TTL in seconds
   * @return existing value if present, otherwise {@code null} and the new value is stored
   * @throws CacheOperationException on cache write failure
   */
  V putIfAbsent (K key, V value, int timeToLiveSeconds)
    throws CacheOperationException;

  /**
   * Removes the entry for the given key.
   *
   * @param key key to evict
   * @throws CacheOperationException on cache removal failure
   */
  void remove (K key)
    throws CacheOperationException;
}
