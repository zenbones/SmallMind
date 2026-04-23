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

/**
 * Extension of {@link PersistenceCache} that adds compare-and-swap (CAS) read and write operations
 * for optimistic concurrency control.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface CASSupportingPersistenceCache<K, V> extends PersistenceCache<K, V> {

  /**
   * Indicates whether values must be copied before a distributed CAS write to avoid shared-state
   * corruption.
   *
   * @return {@code true} if the caller must copy the value before invoking {@link #putViaCas}
   */
  boolean requiresCopyOnDistributedCASOperation ();

  /**
   * Fetches a value together with its CAS version token.
   *
   * @param key cache key to look up
   * @return {@link CASValue} containing the current value and version; never {@code null}
   */
  CASValue<V> getViaCas (K key);

  /**
   * Conditionally stores a new value if the supplied version matches the current cache version.
   *
   * @param key               cache key to update
   * @param oldValue          previous value, available for implementations that need it
   * @param value             new value to store
   * @param version           CAS version token obtained from a prior {@link #getViaCas} call
   * @param timeToLiveSeconds TTL in seconds for the updated entry
   * @return {@code true} if the swap succeeded; {@code false} if the version did not match
   * @throws CacheOperationException if the underlying cache operation fails
   */
  boolean putViaCas (K key, V oldValue, V value, long version, int timeToLiveSeconds)
    throws CacheOperationException;
}
