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
import org.smallmind.persistence.cache.WideVectorAwareDao;
import org.smallmind.persistence.cache.WideVectoredDao;

/**
 * Managed DAO variant that works with wide-vector caches keyed by a parent identifier.
 *
 * @param <W> parent identifier type
 * @param <I> durable identifier type
 * @param <D> durable type managed by the DAO
 */
public abstract class AbstractWideVectorAwareManagedDao<W extends Serializable & Comparable<W>, I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractManagedDao<I, D> implements WideVectorAwareDao<W, I, D> {

  private final WideVectoredDao<W, I, D> wideVectoredDao;

  /**
   * Constructs a managed DAO with a cache delegate for wide vectors.
   *
   * @param metricSource    the metric source name for this DAO
   * @param wideVectoredDao the cache-backed delegate used when caching is enabled
   */
  public AbstractWideVectorAwareManagedDao (String metricSource, WideVectoredDao<W, I, D> wideVectoredDao) {

    super(metricSource);

    this.wideVectoredDao = wideVectoredDao;
  }

  /**
   * Indicates whether caching should be used when accessing wide vectors.
   *
   * @return {@code true} when cache lookups are enabled
   */
  public abstract boolean isCacheEnabled ();

  /**
   * Returns the cache-aware wide vector delegate when caching is enabled.
   *
   * @return the configured {@link WideVectoredDao} or {@code null} when caching is disabled
   */
  @Override
  public WideVectoredDao<W, I, D> getWideVectoredDao () {

    return isCacheEnabled() ? wideVectoredDao : null;
  }
}
