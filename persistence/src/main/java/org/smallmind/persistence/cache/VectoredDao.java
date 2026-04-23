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
 * DAO contract that extends basic CRUD with vector cache management, covering bulk lookup, vector
 * creation, migration, persistence, and eviction.
 *
 * @param <I> durable identifier type
 * @param <D> durable type
 */
public interface VectoredDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends Dao<I, D> {

  /**
   * Returns the identifier used to label metrics emitted by this DAO.
   *
   * @return metrics source string
   */
  String getMetricSource ();

  /**
   * Fetches multiple durables by their cache keys in a single call.
   *
   * @param durableClass durable class for all requested keys
   * @param durableKeys  list of keys identifying the durables to retrieve
   * @return map of {@link DurableKey} to found durable; keys with no entry are absent
   */
  Map<DurableKey<I, D>, D> get (Class<D> durableClass, List<DurableKey<I, D>> durableKeys);

  /**
   * Persists a durable and updates caches according to the supplied update mode.
   *
   * @param durableClass durable class being persisted
   * @param durable      durable instance to store
   * @param mode         controls how the instance and vector caches are updated
   * @return the persisted durable instance
   */
  D persist (Class<D> durableClass, D durable, UpdateMode mode);

  /**
   * Merges an updated durable into any existing cached vector identified by the key.
   *
   * @param vectorKey key identifying the vector to update
   * @param durable   updated durable to merge into the vector
   */
  void updateInVector (VectorKey<D> vectorKey, D durable);

  /**
   * Removes a durable from the cached vector identified by the key.
   *
   * @param vectorKey key identifying the vector to modify
   * @param durable   durable instance to remove from the vector
   */
  void removeFromVector (VectorKey<D> vectorKey, D durable);

  /**
   * Returns the cached vector for the given key.
   *
   * @param vectorKey key identifying the vector to retrieve
   * @return the cached {@link DurableVector}, or {@code null} if absent
   */
  DurableVector<I, D> getVector (VectorKey<D> vectorKey);

  /**
   * Stores a vector in the cache if no entry exists for the key, then returns the effective cached
   * vector.
   *
   * @param vectorKey key under which the vector should be stored
   * @param vector    vector to store
   * @return the pre-existing cached vector if one was present, otherwise the supplied vector
   */
  DurableVector<I, D> persistVector (VectorKey<D> vectorKey, DurableVector<I, D> vector);

  /**
   * Converts a vector to the correct concrete implementation for the managed durable class,
   * preserving ordering and TTL settings.
   *
   * @param managedClass durable class for which the vector is being migrated
   * @param vector       source vector to migrate
   * @return vector in the expected concrete type for {@code managedClass}
   */
  DurableVector<I, D> migrateVector (Class<D> managedClass, DurableVector<I, D> vector);

  /**
   * Creates a vector containing exactly one durable element.
   *
   * @param vectorKey         key that will identify this vector in the cache
   * @param durable           single durable to include in the vector
   * @param timeToLiveSeconds TTL in seconds for the vector
   * @return newly created singular vector
   */
  DurableVector<I, D> createSingularVector (VectorKey<D> vectorKey, D durable, int timeToLiveSeconds);

  /**
   * Creates a vector from an iterable of durables with the specified ordering and size constraints.
   *
   * @param vectorKey         key that will identify this vector in the cache
   * @param elementIter       source of durable elements to include
   * @param comparator        element comparator for ordering; may be {@code null}
   * @param maxSize           maximum number of elements; {@code 0} for unbounded
   * @param timeToLiveSeconds TTL in seconds for the vector
   * @param ordered           {@code true} if element order should be maintained
   * @return newly created vector populated with elements from {@code elementIter}
   */
  DurableVector<I, D> createVector (VectorKey<D> vectorKey, Iterable<D> elementIter, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered);

  /**
   * Evicts the cached vector identified by the given key.
   *
   * @param vectorKey key of the vector to remove from the cache
   */
  void deleteVector (VectorKey<D> vectorKey);
}
