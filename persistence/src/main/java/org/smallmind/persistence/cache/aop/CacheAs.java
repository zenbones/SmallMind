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
package org.smallmind.persistence.cache.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * Marks a data access method whose return value should be cached under a calculated vector key.
 * Methods annotated with {@link CacheAs} will have their results stored in a cache so subsequent
 * invocations can reuse the materialized durable instances when available.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheAs {

  /**
   * Identifies the cache vector that the annotated method should populate or read.
   *
   * @return vector metadata describing how the cache key should be constructed
   */
  Vector value ();

  /**
   * Defines the comparator used to order cached collections.
   *
   * @return comparator class applied when the vector is ordered
   */
  Class<? extends Comparator> comparator () default Comparator.class;

  /**
   * Maximum number of elements retained in the cached vector.
   *
   * @return upper bound for cached results, or zero for no limit
   */
  int max () default 0;

  /**
   * Time-to-live configuration for the cache entry.
   *
   * @return duration descriptor; zero delegates to the cache domain default
   */
  Time time () default @Time(0);

  /**
   * Indicates whether the resulting vector should be stored in sorted order.
   *
   * @return {@code true} to keep cached results ordered using the provided comparator
   */
  boolean ordered () default false;
}
