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
 * {@link DurableVector} for intrinsic (in-process) caches that stores element references as
 * {@link DurableKey} values in an {@link IntrinsicRoster}. Keys are resolved against the local
 * cache or DAO when the vector is iterated or accessed.
 *
 * @param <I> the identifier type, which must be {@link Serializable} and {@link Comparable}
 * @param <D> the durable type
 */
public class ByKeyIntrinsicVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractDurableVector<I, D> {

  private final ByKeyRoster<I, D> roster;

  /**
   * Builds a keyed intrinsic vector populated from the provided durables.
   * When {@code maxSize} is positive, population stops after that many elements.
   *
   * @param durableClass      the class of the durables being stored
   * @param durables          the initial elements
   * @param comparator        comparator for ordered vectors; {@code null} uses natural ordering
   * @param maxSize           maximum number of elements to retain; zero or negative means unbounded
   * @param timeToLiveSeconds the TTL for the vector in seconds
   * @param ordered           {@code true} to maintain elements in sorted order
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
   * Internal copy constructor used by {@link #copy()}.
   *
   * @param roster            the roster to back the new vector
   * @param comparator        comparator for ordering
   * @param maxSize           maximum element count
   * @param timeToLiveSeconds TTL in seconds
   * @param ordered           whether to maintain sorted order
   */
  private ByKeyIntrinsicVector (ByKeyRoster<I, D> roster, Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    super(comparator, maxSize, timeToLiveSeconds, ordered);

    this.roster = roster;
  }

  /**
   * Returns the backing key-based roster.
   *
   * @return the roster of keyed entries
   */
  public Roster<D> getRoster () {

    return roster;
  }

  /**
   * Produces a deep copy of this vector, duplicating the roster and all configuration.
   *
   * @return a new vector with identical state
   */
  public DurableVector<I, D> copy () {

    return new ByKeyIntrinsicVector<>(new ByKeyRoster<>(roster.getDurableClass(), new IntrinsicRoster<>(roster.getInternalRoster())), getComparator(), getMaxSize(), getTimeToLiveSeconds(), isOrdered());
  }

  /**
   * Returns an unmodifiable snapshot of the current roster contents without prefetching.
   *
   * @return an unmodifiable list of the current durables
   */
  public synchronized List<D> asBestEffortLazyList () {

    return Collections.unmodifiableList(new LinkedList<>(roster));
  }

  /**
   * Returns an unmodifiable list of durables hydrated via the vector cache or DAO.
   *
   * @return a prefetched unmodifiable list
   */
  public synchronized List<D> asBestEffortPreFetchedList () {

    return Collections.unmodifiableList(roster.prefetch());
  }

  /**
   * Returns an iterator over an unmodifiable snapshot of the current roster.
   *
   * @return an iterator over the current durables
   */
  public synchronized Iterator<D> iterator () {

    return Collections.unmodifiableList(new LinkedList<>(roster)).iterator();
  }
}
