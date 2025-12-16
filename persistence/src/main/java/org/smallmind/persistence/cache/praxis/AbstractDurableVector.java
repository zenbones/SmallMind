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
import java.util.Comparator;
import java.util.Iterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;

/**
 * Base implementation of {@link DurableVector} backed by a {@link Roster}. Concrete subclasses
 * supply roster storage and copy semantics while this class manages ordering, uniqueness, and sizing.
 *
 * @param <I> identifier type
 * @param <D> durable type
 */
public abstract class AbstractDurableVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  /**
   * Creates a vector with the supplied ordering and sizing rules.
   *
   * @param comparator        comparator used for ordering; {@code null} falls back to natural ordering
   * @param maxSize           maximum number of entries to retain; zero or less means unbounded
   * @param timeToLiveSeconds time-to-live for the vector
   * @param ordered           whether elements should be maintained in sorted order
   */
  public AbstractDurableVector (Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    super(comparator, maxSize, timeToLiveSeconds, ordered);
  }

  /**
   * @return backing roster storing elements of this vector
   */
  public abstract Roster<D> getRoster ();

  /**
   * @return a deep copy of the vector state
   */
  public abstract DurableVector<I, D> copy ();

  /**
   * Indicates whether this vector is restricted to a single element.
   *
   * @return {@code true} for singular vectors
   */
  public boolean isSingular () {

    return false;
  }

  /**
   * Adds the supplied durable to the vector respecting ordering, uniqueness, and max size rules.
   *
   * @param durable element to add
   * @return {@code true} when the vector changes
   */
  public synchronized boolean add (D durable) {

    boolean changed = false;

    if (durable != null) {

      if (isOrdered()) {

        Iterator<D> rosterIter = getRoster().iterator();
        D element;
        boolean matched = false;
        boolean zoned = false;
        boolean inserted = false;
        int index = 0;

        while ((!(matched && zoned)) && rosterIter.hasNext()) {
          element = rosterIter.next();

          if (element.equals(durable)) {
            if (((getComparator() == null) ? element.compareTo(durable) : getComparator().compare(element, durable)) == 0) {
              zoned = true;
              inserted = true;
            } else {
              changed = true;
              rosterIter.remove();
            }

            matched = true;
          } else if ((!zoned) && ((getComparator() == null) ? element.compareTo(durable) : getComparator().compare(element, durable)) >= 0) {
            zoned = true;
          } else if (!zoned) {
            index++;
          }
        }

        if (!inserted) {
          changed = true;
          getRoster().add(index, durable);
        }
      } else {

        boolean matched = false;

        for (D element : getRoster()) {
          if (element.equals(durable)) {
            matched = true;
            break;
          }
        }

        if (!matched) {
          changed = true;
          getRoster().addFirst(durable);
        }
      }

      if ((getMaxSize() > 0) && (getRoster().size() > getMaxSize())) {
        changed = true;
        getRoster().removeLast();
      }
    }

    return changed;
  }

  /**
   * Removes all occurrences of the supplied durable from the vector.
   *
   * @param durable element to remove
   * @return {@code true} when at least one element is removed
   */
  public synchronized boolean remove (D durable) {

    boolean changed = false;
    boolean removed;

    do {
      if (removed = getRoster().remove(durable)) {
        changed = true;
      }
    } while (removed);

    return changed;
  }

  /**
   * Returns the first element in the roster or {@code null} when empty.
   *
   * @return head of the vector
   */
  public synchronized D head () {

    if (getRoster().isEmpty()) {
      return null;
    }

    return getRoster().get(0);
  }
}
