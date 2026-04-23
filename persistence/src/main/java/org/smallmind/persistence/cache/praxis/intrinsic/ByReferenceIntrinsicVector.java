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
package org.smallmind.persistence.cache.praxis.intrinsic;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.praxis.AbstractDurableVector;

/**
 * {@link DurableVector} for intrinsic (in-process) caches that stores direct object references to
 * durable instances in an {@link IntrinsicRoster}. No key resolution is required on access.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByReferenceIntrinsicVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractDurableVector<I, D> {

  private final IntrinsicRoster<D> roster;

  /**
   * Creates a reference-based intrinsic vector backed by the provided roster.
   * Excess elements beyond {@code maxSize} are removed from the tail immediately.
   *
   * @param roster            the roster containing the initial durable references
   * @param comparator        comparator for ordered vectors; {@code null} uses natural ordering
   * @param maxSize           maximum number of elements to retain; zero or negative means unbounded
   * @param timeToLiveSeconds the TTL for the vector in seconds
   * @param ordered           {@code true} to maintain elements in sorted order
   */
  public ByReferenceIntrinsicVector (IntrinsicRoster<D> roster, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    super(comparator, maxSize, timeToLiveSeconds, ordered);

    this.roster = roster;
    if (maxSize > 0) {
      while (roster.size() > maxSize) {
        roster.removeLast();
      }
    }
  }

  /**
   * Returns the backing {@link IntrinsicRoster} for this vector.
   *
   * @return the roster of durable references
   */
  @Override
  public IntrinsicRoster<D> getRoster () {

    return roster;
  }

  /**
   * Creates a deep copy of this vector with a cloned roster.
   *
   * @return a new vector with the same configuration and a copy of the roster contents
   */
  public DurableVector<I, D> copy () {

    return new ByReferenceIntrinsicVector<>(new IntrinsicRoster<D>(roster), getComparator(), getMaxSize(), getTimeToLiveSeconds(), isOrdered());
  }

  /**
   * Returns an unmodifiable view of the current roster without any prefetching.
   *
   * @return an unmodifiable list backed by the current roster
   */
  public synchronized List<D> asBestEffortLazyList () {

    return Collections.unmodifiableList(getRoster());
  }

  /**
   * Returns an iterator over an unmodifiable view of the current roster.
   *
   * @return an iterator over the roster elements
   */
  public synchronized Iterator<D> iterator () {

    return Collections.unmodifiableList(getRoster()).iterator();
  }
}
