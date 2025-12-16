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
package org.smallmind.persistence;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * DAO contract tailored to {@link Durable} entities, providing CRUD and batch retrieval helpers.
 *
 * @param <I> the durable identifier type
 * @param <D> the durable type
 */
public interface DurableDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends Dao<I, D> {

  /**
   * Finds a durable by id.
   *
   * @param id the identifier of the durable
   * @return the matching durable instance, or {@code null} when not found
   * @throws PersistenceException if the lookup fails
   */
  D get (I id);

  /**
   * Persists the given durable, updating it if already present or inserting it otherwise.
   *
   * @param durable the durable to persist
   * @return the stored durable (potentially a different instance depending on the implementation)
   * @throws PersistenceException if the persist operation fails
   */
  D persist (D durable);

  /**
   * Persists the given durable, explicitly providing the durable class.
   *
   * @param durableClass the declared durable class
   * @param durable      the durable to persist
   * @return the stored durable
   * @throws PersistenceException if the persist operation fails
   */
  D persist (Class<D> durableClass, D durable);

  /**
   * Deletes the supplied durable.
   *
   * @param durable the durable to remove
   * @throws PersistenceException if the delete operation fails
   */
  void delete (D durable);

  /**
   * Lists all durable instances.
   *
   * @return the full collection of durables
   * @throws PersistenceException if retrieval fails
   */
  List<D> list ();

  /**
   * Lists durable instances, capped by the given fetch size.
   *
   * @param fetchSize the maximum number of records to return
   * @return a limited list of durables
   * @throws PersistenceException if retrieval fails
   */
  List<D> list (int fetchSize);

  /**
   * Lists durable instances with identifiers greater than the supplied value, up to the fetch size.
   *
   * @param greaterThan the lower bound identifier (exclusive)
   * @param fetchSize   the maximum number of records to return
   * @return a limited list of durables beyond the supplied id
   * @throws PersistenceException if retrieval fails
   */
  List<D> list (I greaterThan, int fetchSize);

  /**
   * Retrieves a collection of durables whose identifiers are included in the provided set.
   *
   * @param idCollection identifiers to search for
   * @return the durables that match the requested identifiers
   * @throws PersistenceException if retrieval fails
   */
  List<D> list (Collection<I> idCollection);
}
