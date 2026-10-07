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
package org.smallmind.nutsnbolts.lang;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A concurrent cache partitioned by the class each key belongs to. Each partition is attached to its class through a
 * {@link ClassValue}, so the cache never keeps a class, or its class loader, reachable: a partition and its entries
 * become collectible along with the class. A value that references classes from a different class loader keeps that
 * loader reachable for as long as the key's class is reachable.
 *
 * @param <K> the type of keys stored in the cache
 * @param <V> the type of values stored in the cache
 */
public class ClassLoaderAwareCache<K, V> {

  private final ClassValue<ConcurrentHashMap<K, V>> partitionValue = new ClassValue<>() {

    @Override
    protected ConcurrentHashMap<K, V> computeValue (Class<?> type) {

      return new ConcurrentHashMap<>();
    }
  };
  private final Function<K, Class<?>> classExtractor;

  /**
   * Creates a cache that uses the supplied function to determine which class partition each key belongs to.
   *
   * @param classExtractor a function that maps a cache key to the class whose lifetime bounds the key's entry;
   *                       it must not return {@code null}
   */
  public ClassLoaderAwareCache (Function<K, Class<?>> classExtractor) {

    this.classExtractor = classExtractor;
  }

  /**
   * Returns the cached value for the given key within the key's class partition, or {@code null} if absent.
   *
   * @param key the cache key
   * @return the cached value, or {@code null} if no mapping exists
   * @throws NullPointerException if the class extractor returns {@code null} for the key
   */
  public V get (K key) {

    return getMap(key).get(key);
  }

  /**
   * Associates the given value with the key in the key's class partition, replacing any existing mapping.
   *
   * @param key   the cache key
   * @param value the value to associate with the key
   * @return the previous value associated with the key, or {@code null} if there was no prior mapping
   * @throws NullPointerException if the class extractor returns {@code null} for the key
   */
  public V put (K key, V value) {

    return getMap(key).put(key, value);
  }

  /**
   * Associates the given value with the key in the key's class partition only if no mapping currently exists.
   *
   * @param key   the cache key
   * @param value the value to store if no mapping is present
   * @return the existing value if one was already present, or {@code null} if the new value was stored
   * @throws NullPointerException if the class extractor returns {@code null} for the key
   */
  public V putIfAbsent (K key, V value) {

    return getMap(key).putIfAbsent(key, value);
  }

  /**
   * Returns the map for the class partition associated with the given key, creating it lazily if needed.
   *
   * @param key the key whose class partition is required
   * @return the concurrent map for the partition corresponding to the key's class
   * @throws NullPointerException if the class extractor returns {@code null} for the key
   */
  private ConcurrentHashMap<K, V> getMap (K key) {

    return partitionValue.get(classExtractor.apply(key));
  }
}
