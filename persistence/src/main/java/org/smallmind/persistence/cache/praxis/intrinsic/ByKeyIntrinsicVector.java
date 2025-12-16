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
import java.util.LinkedList;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.praxis.AbstractDurableVector;
import org.smallmind.persistence.cache.praxis.ByKeyRoster;
import org.smallmind.persistence.cache.praxis.Roster;

/**
 * Durable vector for intrinsic caches where elements are referenced by durable keys.
 * Keys are resolved against the local cache or DAO when iterated.
 *
 * @param <I> identifier type
 * @param <D> durable type
 */
public class ByKeyIntrinsicVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractDurableVector<I, D> {

  private final ByKeyRoster<I, D> roster;

  /**
   * Builds a keyed vector from an iterable of durables.
   *
   * @param durableClass      durable class stored in the vector
   * @param durables          iterable of durables used to seed the vector
   * @param comparator        comparator used for ordering; {@code null} for natural order
   * @param maxSize           maximum number of elements to retain
   * @param timeToLiveSeconds TTL for the vector
   * @param ordered           whether the vector should maintain sorted order
   */
  public ByKeyIntrinsicVector (Class<D> durableClass, Iterable<D> durables, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    super(comparator, maxSize, timeToLiveSeconds, ordered);

    IntrinsicRoster<DurableKey<I, D>> keyRoster = new IntrinsicRoster<>();
    int index = 0;

    for (D durable : durables) {
      keyRoster.add(new DurableKey<>(durableClass, durable.getId()));
      if ((maxSize > 0) && (++index == maxSize)) {
        break;
      }
    }

    roster = new ByKeyRoster<>(durableClass, keyRoster);
  }

  /**
   * Internal constructor used when copying vectors.
   */
  private ByKeyIntrinsicVector (ByKeyRoster<I, D> roster, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    super(comparator, maxSize, timeToLiveSeconds, ordered);

    this.roster = roster;
  }

  /**
   * @return backing roster of keys
   */
  public Roster<D> getRoster () {

    return roster;
  }

  /**
   * Produces a deep copy of the vector and its roster.
   *
   * @return copied vector
   */
  public DurableVector<I, D> copy () {

    return new ByKeyIntrinsicVector<>(new ByKeyRoster<>(roster.getDurableClass(), new IntrinsicRoster<>(roster.getInternalRoster())), getComparator(), getMaxSize(), getTimeToLiveSeconds(), isOrdered());
  }

  /**
   * @return unmodifiable list backed by a snapshot of the roster
   */
  public synchronized List<D> asBestEffortLazyList () {

    return Collections.unmodifiableList(new LinkedList<>(roster));
  }

  /**
   * Hydrates elements and returns an unmodifiable list.
   *
   * @return prefetched unmodifiable list
   */
  public synchronized List<D> asBestEffortPreFetchedList () {

    return Collections.unmodifiableList(roster.prefetch());
  }

  /**
   * @return iterator over an unmodifiable snapshot of the roster
   */
  public synchronized Iterator<D> iterator () {

    return Collections.unmodifiableList(new LinkedList<>(roster)).iterator();
  }
}
