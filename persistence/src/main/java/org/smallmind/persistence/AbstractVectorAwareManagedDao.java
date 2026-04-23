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
import org.smallmind.persistence.cache.VectorAwareDao;
import org.smallmind.persistence.cache.VectoredDao;

/**
 * {@link AbstractManagedDao} extension that integrates with a cache-backed {@link VectoredDao}.
 * Subclasses control whether caching is active by implementing {@link #isCacheEnabled()}.
 *
 * @param <I> the durable identifier type
 * @param <D> the durable entity type managed by this DAO
 */
public abstract class AbstractVectorAwareManagedDao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractManagedDao<I, D> implements VectorAwareDao<I, D> {

  private final VectoredDao<I, D> vectoredDao;

  /**
   * Constructs the DAO with a metric source label and an optional cache delegate.
   *
   * @param metricSource the label used to attribute metrics for this DAO
   * @param vectoredDao  the cache-backed delegate returned when caching is enabled
   */
  public AbstractVectorAwareManagedDao (String metricSource, VectoredDao<I, D> vectoredDao) {

    super(metricSource);

    this.vectoredDao = vectoredDao;
  }

  /**
   * Determines whether the cache delegate should be used for DAO operations.
   *
   * @return {@code true} when caching is active
   */
  public abstract boolean isCacheEnabled ();

  /**
   * Returns the {@link VectoredDao} when {@link #isCacheEnabled()} is {@code true},
   * or {@code null} when caching is disabled.
   *
   * @return the cache delegate, or {@code null}
   */
  @Override
  public VectoredDao<I, D> getVectoredDao () {

    return isCacheEnabled() ? vectoredDao : null;
  }
}
