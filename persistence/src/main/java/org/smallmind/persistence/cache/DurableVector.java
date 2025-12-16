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
 * Base representation of a cached vector of durables with optional ordering, TTL, and size
 * constraints.
 */
public abstract class DurableVector<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Serializable, Iterable<D> {

  private final Comparator<D> comparator;
  private final boolean ordered;
  private final long creationTimeMilliseconds;
  private final int timeToLiveSeconds;
  private final int maxSize;

  /**
   * @param comparator        optional comparator for ordering
   * @param maxSize           maximum number of elements (0 for unlimited)
   * @param timeToLiveSeconds TTL in seconds (0 or less for infinite)
   * @param ordered           whether ordering matters for this vector
   */
  public DurableVector (Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    this.comparator = comparator;
    this.maxSize = maxSize;
    this.timeToLiveSeconds = timeToLiveSeconds;
    this.ordered = ordered;

    creationTimeMilliseconds = System.currentTimeMillis();
  }

  /**
   * Creates a defensive copy of this vector, preserving ordering and TTL semantics.
   *
   * @return duplicated vector instance
   */
  public abstract DurableVector<I, D> copy ();

  /**
   * Indicates whether the vector contains exactly one element.
   *
   * @return {@code true} when only a single durable is present
   */
  public abstract boolean isSingular ();

  /**
   * Adds the durable to the vector respecting ordering/size semantics.
   *
   * @param durable durable to add
   * @return true if the vector changed
   */
  public abstract boolean add (D durable);

  /**
   * Removes the durable from the vector.
   *
   * @param durable durable to remove
   * @return true if removed
   */
  public abstract boolean remove (D durable);

  /**
   * @return head element of the vector or {@code null}
   */
  public abstract D head ();

  /**
   * @return list view of the vector; may fetch lazily
   */
  public abstract List<D> asBestEffortLazyList ();

  /**
   * @return eagerly fetched list view of the vector when supported
   */
  public List<D> asBestEffortPreFetchedList () {

    return asBestEffortLazyList();
  }

  /**
   * @return comparator used for ordering (may be null)
   */
  public Comparator<D> getComparator () {

    return comparator;
  }

  /**
   * @return maximum size of the vector (0 for unbounded)
   */
  public int getMaxSize () {

    return maxSize;
  }

  /**
   * @return TTL in seconds
   */
  public int getTimeToLiveSeconds () {

    return timeToLiveSeconds;
  }

  /**
   * @return whether ordering is meaningful for this vector
   */
  public boolean isOrdered () {

    return ordered;
  }

  /**
   * Determines if the vector has expired based on its TTL.
   *
   * @return true if still valid
   */
  public boolean isAlive () {

    return (timeToLiveSeconds <= 0) || ((System.currentTimeMillis() - creationTimeMilliseconds) / 1000 <= timeToLiveSeconds);
  }
}
