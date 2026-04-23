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
import org.smallmind.persistence.Durable;

/**
 * Abstract base for a cached, optionally ordered collection of durable instances with a bounded
 * size and TTL-based expiry.
 *
 * @param <I> durable identifier type
 * @param <D> durable type
 */
public abstract class DurableVector<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Serializable, Iterable<D> {

  private final Comparator<D> comparator;
  private final boolean ordered;
  private final long creationTimeMilliseconds;
  private final int timeToLiveSeconds;
  private final int maxSize;

  /**
   * Initialises a new vector with the provided constraints.
   *
   * @param comparator        comparator for element ordering; may be {@code null}
   * @param maxSize           maximum number of elements retained; {@code 0} means unbounded
   * @param timeToLiveSeconds how long the vector remains alive in seconds; {@code 0} or negative means infinite
   * @param ordered           {@code true} if element order is meaningful for this vector
   */
  public DurableVector (Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    this.comparator = comparator;
    this.maxSize = maxSize;
    this.timeToLiveSeconds = timeToLiveSeconds;
    this.ordered = ordered;

    creationTimeMilliseconds = System.currentTimeMillis();
  }

  /**
   * Returns a deep copy of this vector, preserving all ordering, TTL, and size settings.
   *
   * @return new vector instance with the same contents and configuration
   */
  public abstract DurableVector<I, D> copy ();

  /**
   * Returns {@code true} if the vector contains exactly one element.
   *
   * @return {@code true} when the vector is singular
   */
  public abstract boolean isSingular ();

  /**
   * Adds a durable to the vector, respecting ordering and size constraints.
   *
   * @param durable durable instance to add
   * @return {@code true} if the vector was modified
   */
  public abstract boolean add (D durable);

  /**
   * Removes a durable from the vector.
   *
   * @param durable durable instance to remove
   * @return {@code true} if the vector was modified
   */
  public abstract boolean remove (D durable);

  /**
   * Returns the first element of the vector without removing it.
   *
   * @return head element, or {@code null} if the vector is empty
   */
  public abstract D head ();

  /**
   * Returns a list view of the vector's contents, potentially fetching elements lazily.
   *
   * @return list of durables in this vector
   */
  public abstract List<D> asBestEffortLazyList ();

  /**
   * Returns a list view of the vector's contents, eagerly pre-fetching when the implementation
   * supports it; falls back to {@link #asBestEffortLazyList()} by default.
   *
   * @return eagerly populated list of durables
   */
  public List<D> asBestEffortPreFetchedList () {

    return asBestEffortLazyList();
  }

  /**
   * Returns the comparator used to order elements in this vector.
   *
   * @return element comparator, or {@code null} if unordered
   */
  public Comparator<D> getComparator () {

    return comparator;
  }

  /**
   * Returns the maximum number of elements this vector may hold.
   *
   * @return max size; {@code 0} indicates unbounded
   */
  public int getMaxSize () {

    return maxSize;
  }

  /**
   * Returns the configured time-to-live for this vector.
   *
   * @return TTL in seconds
   */
  public int getTimeToLiveSeconds () {

    return timeToLiveSeconds;
  }

  /**
   * Returns whether element ordering is meaningful for this vector.
   *
   * @return {@code true} if the vector maintains a defined element order
   */
  public boolean isOrdered () {

    return ordered;
  }

  /**
   * Returns {@code true} if this vector has not yet exceeded its TTL.
   *
   * @return {@code true} if the vector is still valid; {@code false} if expired
   */
  public boolean isAlive () {

    return (timeToLiveSeconds <= 0) || ((System.currentTimeMillis() - creationTimeMilliseconds) / 1000 <= timeToLiveSeconds);
  }
}
