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
 * Singular {@link DurableVector} that directly references a durable instance.
 *
 * @param <I> identifier type
 * @param <D> durable type
 */
public class ByReferenceSingularVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  private D durable;

  /**
   * Creates a singular vector referencing the provided durable.
   *
   * @param durable           durable instance to store
   * @param timeToLiveSeconds TTL for the vector
   */
  public ByReferenceSingularVector (D durable, int timeToLiveSeconds) {

    super(null, 1, timeToLiveSeconds, false);

    this.durable = durable;
  }

  /**
   * @return copy of this vector retaining the durable reference and TTL
   */
  public DurableVector<I, D> copy () {

    return new ByReferenceSingularVector<>(durable, getTimeToLiveSeconds());
  }

  /**
   * @return {@code true} because this vector always stores one element
   */
  public boolean isSingular () {

    return true;
  }

  /**
   * Replaces the stored durable when it differs from the current one.
   *
   * @param durable durable to store
   * @return {@code true} when the reference is updated
   */
  public synchronized boolean add (D durable) {

    if (!this.durable.equals(durable)) {
      this.durable = durable;

      return true;
    }

    return false;
  }

  /**
   * Removal is unsupported for singular vectors.
   *
   * @param durable unused
   * @return never returns; always throws {@link UnsupportedOperationException}
   */
  public boolean remove (D durable) {

    throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
  }

  /**
   * @return current durable reference
   */
  public synchronized D head () {

    return durable;
  }

  /**
   * @return singleton list containing the stored durable
   */
  public synchronized List<D> asBestEffortLazyList () {

    return Collections.singletonList(durable);
  }

  /**
   * @return iterator that yields the stored durable once
   */
  public synchronized Iterator<D> iterator () {

    return new SingleItemIterable<>(durable).iterator();
  }
}
