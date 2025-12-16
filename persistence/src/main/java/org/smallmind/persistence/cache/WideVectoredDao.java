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
import java.util.List;
import org.smallmind.persistence.Durable;

/**
 * DAO contract for handling wide (list) vectors keyed by a parent durable and optional context.
 *
 * @param <W> parent durable identifier type
 * @param <I> child durable identifier type
 * @param <D> child durable type
 */
public interface WideVectoredDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> {

  /**
   * @return identifier used for metrics
   */
  String getMetricSource ();

  /**
   * Retrieves a wide list of child durables from storage/cache.
   *
   * @param context      optional context key
   * @param parentClass  parent durable class
   * @param parentId     parent identifier
   * @param durableClass child durable class
   * @return list of children or {@code null}
   */
  List<D> get (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);

  /**
   * Persists the provided list of child durables for the given parent/context.
   *
   * @param context      optional context key
   * @param parentClass  parent durable class
   * @param parentId     parent identifier
   * @param durableClass child durable class
   * @param durables     children to persist
   * @return persisted list (may be cached version)
   */
  List<D> persist (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass, List<D> durables);

  /**
   * Deletes the cached/managed wide list for the given composite key.
   *
   * @param context      optional context key
   * @param parentClass  parent durable class
   * @param parentId     parent identifier
   * @param durableClass child durable class
   */
  void delete (String context, Class<? extends Durable<W>> parentClass, W parentId, Class<D> durableClass);
}
