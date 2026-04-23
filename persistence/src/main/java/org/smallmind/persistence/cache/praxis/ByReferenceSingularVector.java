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
package org.smallmind.persistence.cache.praxis;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.smallmind.nutsnbolts.util.SingleItemIterable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;

/**
 * Singular {@link DurableVector} that holds a direct object reference to a single durable instance.
 * Unlike key-based singular vectors, no DAO lookup is performed on access.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByReferenceSingularVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  private D durable;

  /**
   * Creates a singular vector holding the provided durable instance.
   *
   * @param durable           the durable to store
   * @param timeToLiveSeconds the TTL for this vector in seconds
   */
  public ByReferenceSingularVector (D durable, int timeToLiveSeconds) {

    super(null, 1, timeToLiveSeconds, false);

    this.durable = durable;
  }

  /**
   * Returns a copy of this vector retaining the same durable reference and TTL.
   *
   * @return a new {@link ByReferenceSingularVector} with the same state
   */
  public DurableVector<I, D> copy () {

    return new ByReferenceSingularVector<>(durable, getTimeToLiveSeconds());
  }

  /**
   * Returns {@code true} because this vector always holds exactly one element.
   *
   * @return {@code true}
   */
  public boolean isSingular () {

    return true;
  }

  /**
   * Replaces the stored durable when the supplied instance differs from the current one.
   *
   * @param durable the durable to store
   * @return {@code true} when the stored reference is updated
   */
  public synchronized boolean add (D durable) {

    if (!this.durable.equals(durable)) {
      this.durable = durable;

      return true;
    }

    return false;
  }

  /**
   * Removal is not supported for singular vectors.
   *
   * @param durable unused
   * @return never returns normally
   * @throws UnsupportedOperationException always
   */
  public boolean remove (D durable) {

    throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
  }

  /**
   * Returns the stored durable instance.
   *
   * @return the current durable reference
   */
  public synchronized D head () {

    return durable;
  }

  /**
   * Returns a singleton list containing the stored durable.
   *
   * @return an unmodifiable single-element list
   */
  public synchronized List<D> asBestEffortLazyList () {

    return Collections.singletonList(durable);
  }

  /**
   * Returns an iterator that yields the stored durable exactly once.
   *
   * @return a single-element iterator
   */
  public synchronized Iterator<D> iterator () {

    return new SingleItemIterable<>(durable).iterator();
  }
}
