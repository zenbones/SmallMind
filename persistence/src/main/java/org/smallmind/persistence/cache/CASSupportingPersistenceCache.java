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
 * Persistence cache that supports compare-and-swap (CAS) semantics.
 */
public interface CASSupportingPersistenceCache<K, V> extends PersistenceCache<K, V> {

  /**
   * Indicates whether CAS operations require copies to be made to avoid shared-state issues.
   *
   * @return true if caller should copy values before CAS
   */
  boolean requiresCopyOnDistributedCASOperation ();

  /**
   * Retrieves a value along with its CAS version.
   *
   * @param key cache key
   * @return CAS wrapper containing value and version
   */
  CASValue<V> getViaCas (K key);

  /**
   * Attempts to update the value if the supplied version matches.
   *
   * @param key               cache key
   * @param oldValue          previous value (may be used by implementations)
   * @param value             new value to store
   * @param version           expected version
   * @param timeToLiveSeconds TTL in seconds
   * @return true if updated
   * @throws CacheOperationException on cache error
   */
  boolean putViaCas (K key, V oldValue, V value, long version, int timeToLiveSeconds)
    throws CacheOperationException;
}
