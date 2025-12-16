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
import java.util.List;

/**
 * DAO contract for managing child durables that belong to a wide parent identifier and context.
 *
 * @param <W> parent identifier type
 * @param <I> child durable identifier type
 * @param <D> child durable type
 */
public interface WideDurableDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> {

  /**
   * Removes all children for the specified parent and context.
   *
   * @param context     a logical namespace for the collection
   * @param parentClass the parent durable class
   * @param parentId    the identifier of the parent
   * @throws PersistenceException if the removal fails
   */
  void remove (String context, Class<? extends Durable<W>> parentClass, W parentId);

  /**
   * Removes all children of the given durable type for the specified parent and context.
   *
   * @param context      a logical namespace for the collection
   * @param parentClass  the parent durable class
   * @param parentId     the identifier of the parent
   * @param durableClass the child durable class to restrict the removal to
   * @throws PersistenceException if the removal fails
   */
  void remove (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);

  /**
   * Retrieves all children for the specified parent and context.
   *
   * @param context     a logical namespace for the collection
   * @param parentClass the parent durable class
   * @param parentId    the identifier of the parent
   * @return a list of matching children, possibly empty
   * @throws PersistenceException if retrieval fails
   */
  List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId);

  /**
   * Retrieves children of a specific type for the specified parent and context.
   *
   * @param context      a logical namespace for the collection
   * @param parentClass  the parent durable class
   * @param parentId     the identifier of the parent
   * @param durableClass the child durable class to retrieve
   * @return a list of matching children, possibly empty
   * @throws PersistenceException if retrieval fails
   */
  List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);

  /**
   * Persists the provided children under the specified parent and context.
   *
   * @param context     a logical namespace for the collection
   * @param parentClass the parent durable class
   * @param parentId    the identifier of the parent
   * @param durables    the children to persist
   * @return the persisted children
   * @throws PersistenceException if persistence fails
   */
  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables);

  /**
   * Persists the provided list of children under the specified parent and context.
   *
   * @param context     a logical namespace for the collection
   * @param parentClass the parent durable class
   * @param parentId    the identifier of the parent
   * @param durables    the children to persist
   * @return the persisted children
   * @throws PersistenceException if persistence fails
   */
  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables);

  /**
   * Persists the provided children under the specified parent and context, explicitly supplying the durable class.
   *
   * @param context      a logical namespace for the collection
   * @param parentClass  the parent durable class
   * @param parentId     the identifier of the parent
   * @param durableClass the child durable class
   * @param durables     the children to persist
   * @return the persisted children
   * @throws PersistenceException if persistence fails
   */
  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables);

  /**
   * Persists the provided list of children under the specified parent and context, explicitly supplying the durable class.
   *
   * @param context      a logical namespace for the collection
   * @param parentClass  the parent durable class
   * @param parentId     the identifier of the parent
   * @param durableClass the child durable class
   * @param durables     the children to persist
   * @return the persisted children
   * @throws PersistenceException if persistence fails
   */
  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables);

  /**
   * Deletes the supplied children from the specified parent and context.
   *
   * @param context     a logical namespace for the collection
   * @param parentClass the parent durable class
   * @param parentId    the identifier of the parent
   * @param durables    the children to delete
   * @throws PersistenceException if deletion fails
   */
  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, D... durables);

  /**
   * Deletes the supplied list of children from the specified parent and context.
   *
   * @param context     a logical namespace for the collection
   * @param parentClass the parent durable class
   * @param parentId    the identifier of the parent
   * @param durables    the children to delete
   * @throws PersistenceException if deletion fails
   */
  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, List<D> durables);

  /**
   * Deletes the supplied children of the specified durable class from the specified parent and context.
   *
   * @param context      a logical namespace for the collection
   * @param parentClass  the parent durable class
   * @param parentId     the identifier of the parent
   * @param durableClass the child durable class
   * @param durables     the children to delete
   * @throws PersistenceException if deletion fails
   */
  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, D... durables);

  /**
   * Deletes the supplied list of children of the specified durable class from the specified parent and context.
   *
   * @param context      a logical namespace for the collection
   * @param parentClass  the parent durable class
   * @param parentId     the identifier of the parent
   * @param durableClass the child durable class
   * @param durables     the children to delete
   * @throws PersistenceException if deletion fails
   */
  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables);
}
