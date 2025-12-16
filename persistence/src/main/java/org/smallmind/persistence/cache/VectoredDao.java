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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.smallmind.persistence.Dao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;

/**
 * DAO contract that supports vector-based caching alongside CRUD operations.
 *
 * @param <I> durable identifier type
 * @param <D> durable type
 */
public interface VectoredDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends Dao<I, D> {

  /**
   * @return identifier used for metrics reporting
   */
  String getMetricSource ();

  /**
   * Batch lookup of durables by cache key.
   *
   * @param durableClass durable class
   * @param durableKeys  durable keys to fetch
   * @return map of found durables keyed by their {@link DurableKey}
   */
  Map<DurableKey<I, D>, D> get (Class<D> durableClass, List<DurableKey<I, D>> durableKeys);

  /**
   * Persists a durable (implementation-defined) and potentially updates caches based on mode.
   *
   * @param durableClass durable class
   * @param durable      durable to persist
   * @param mode         update mode
   * @return persisted durable
   */
  D persist (Class<D> durableClass, D durable, UpdateMode mode);

  /**
   * Updates the cached vector containing the durable.
   *
   * @param vectorKey vector key
   * @param durable   durable to merge
   */
  void updateInVector (VectorKey<D> vectorKey, D durable);

  /**
   * Removes the durable from the cached vector.
   *
   * @param vectorKey vector key
   * @param durable   durable to remove
   */
  void removeFromVector (VectorKey<D> vectorKey, D durable);

  /**
   * Retrieves a cached vector by key.
   *
   * @param vectorKey vector key
   * @return cached vector or {@code null}
   */
  DurableVector<I, D> getVector (VectorKey<D> vectorKey);

  /**
   * Stores the vector if absent and returns the cached or provided instance.
   *
   * @param vectorKey vector key
   * @param vector    vector to store
   * @return cached vector (either existing or newly stored)
   */
  DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector);

  /**
   * Migrates the vector to the correct concrete type/ordering for the managed class.
   *
   * @param managedClass durable class
   * @param vector       vector to migrate
   * @return migrated vector
   */
  DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector);

  /**
   * Creates a single-element vector for the given durable.
   *
   * @param vectorKey         vector key
   * @param durable           durable element
   * @param timeToLiveSeconds TTL in seconds
   * @return vector containing the element
   */
  DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds);

  /**
   * Creates a vector from the provided elements.
   *
   * @param vectorKey         vector key
   * @param elementIter       elements to include
   * @param comparator        comparator for ordering (may be null)
   * @param maxSize           maximum size (0 for unlimited)
   * @param timeToLiveSeconds TTL in seconds
   * @param ordered           whether order matters
   * @return constructed vector
   */
  DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered);

  /**
   * Deletes the cached vector for the given key.
   *
   * @param vectorKey vector key
   */
  void deleteVector (VectorKey<D> vectorKey);
}
