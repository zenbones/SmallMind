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
package org.smallmind.nutsnbolts.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A size-bounded {@link LinkedHashMap} that evicts the eldest entry whenever the map exceeds its configured maximum size,
 * providing LRU cache semantics.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class LRUMap<K, V> extends LinkedHashMap<K, V> {

  private final int maximumSize;

  /**
   * Constructs an LRU map with insertion-order iteration, a default initial capacity of 100, and a load factor of 0.75.
   *
   * @param maximumSize the maximum number of entries to retain before the eldest is evicted
   */
  public LRUMap (int maximumSize) {

    this(100, .75F, false, maximumSize);
  }

  /**
   * Constructs an LRU map with a configurable iteration order, a default initial capacity of 100, and a load factor of 0.75.
   *
   * @param accessOrder {@code true} for access-order iteration; {@code false} for insertion-order iteration
   * @param maximumSize the maximum number of entries to retain before the eldest is evicted
   */
  public LRUMap (boolean accessOrder, int maximumSize) {

    this(100, .75F, accessOrder, maximumSize);
  }

  /**
   * Constructs an LRU map with full control over capacity, load factor, iteration order, and maximum size.
   *
   * @param initialCapacity the initial capacity of the underlying hash table
   * @param loadFactor      the load factor of the underlying hash table
   * @param accessOrder     {@code true} for access-order iteration; {@code false} for insertion-order iteration
   * @param maximumSize     the maximum number of entries to retain before the eldest is evicted
   */
  public LRUMap (int initialCapacity, float loadFactor, boolean accessOrder, int maximumSize) {

    super(initialCapacity, loadFactor, accessOrder);

    this.maximumSize = maximumSize;
  }

  /**
   * Returns {@code true} to trigger eviction of the eldest entry whenever the map size exceeds the configured maximum.
   *
   * @param eldest the candidate entry for eviction
   * @return {@code true} if the size exceeds {@code maximumSize}
   */
  protected boolean removeEldestEntry (Map.Entry<K, V> eldest) {

    return size() > maximumSize;
  }
}
