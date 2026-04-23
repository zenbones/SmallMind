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
 * Factory interface that provides the instance, wide-instance, and vector caches associated with a
 * particular durable type, along with a metrics source identifier.
 *
 * @param <I> durable identifier type
 * @param <D> durable type
 */
public interface CacheDomain<I extends Serializable & Comparable<I>, D extends Durable<I>> {

  /**
   * Returns the identifier used to label metrics emitted by this domain.
   *
   * @return metrics source string
   */
  String getMetricSource ();

  /**
   * Returns the cache that stores individual durable instances.
   *
   * @param managedClass durable class whose instance cache is required
   * @return persistence cache of single durables keyed by id string
   */
  PersistenceCache<String, D> getInstanceCache (Class<D> managedClass);

  /**
   * Returns the cache that stores wide (list) query results for a parent durable.
   *
   * @param managedClass durable class whose wide-instance cache is required
   * @return persistence cache of durable lists keyed by wide durable key string
   */
  PersistenceCache<String, List<D>> getWideInstanceCache (Class<D> managedClass);

  /**
   * Returns the cache that stores durable vectors.
   *
   * @param managedClass durable class whose vector cache is required
   * @return persistence cache of {@link DurableVector} instances keyed by vector key string
   */
  PersistenceCache<String, DurableVector<I, D>> getVectorCache (Class<D> managedClass);
}
