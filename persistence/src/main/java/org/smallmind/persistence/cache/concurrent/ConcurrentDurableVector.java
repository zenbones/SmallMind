/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.cache.concurrent;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorPredicate;
import org.smallmind.persistence.cache.concurrent.util.Roster;
import org.terracotta.annotations.AutolockRead;
import org.terracotta.annotations.AutolockWrite;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class ConcurrentDurableVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  public ConcurrentDurableVector (Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

    super(comparator, maxSize, timeToLive, ordered);
  }

  public abstract Roster<D> getRoster ();

  public abstract DurableVector<I, D> copy ();

  public boolean isSingular () {

    return false;
  }

  @AutolockWrite
  public synchronized void add (D durable) {

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
            }
            else {
              rosterIter.remove();
            }

            matched = true;
          }
          else if ((!zoned) && ((getComparator() == null) ? element.compareTo(durable) : getComparator().compare(element, durable)) >= 0) {
            zoned = true;
          }
          else if (!zoned) {
            index++;
          }
        }

        if (!inserted) {
          getRoster().add(index, durable);
        }
      }
      else {

        boolean matched = false;

        for (D element : getRoster()) {
          if (element.equals(durable)) {
            matched = true;
            break;
          }
        }

        if (!matched) {
          getRoster().addFirst(durable);
        }
      }

      if ((getMaxSize() > 0) && (getRoster().size() > getMaxSize())) {
        getRoster().removeLast();
      }
    }
  }

  @AutolockWrite
  public synchronized void remove (D durable) {

    boolean removed;

    do {
      removed = getRoster().remove(durable);
    } while (removed);
  }

  @AutolockWrite
  public void removeId (I id) {

    Iterator<D> rosterIter = getRoster().iterator();

    while (rosterIter.hasNext()) {
      if (rosterIter.next().getId().equals(id)) {
        rosterIter.remove();
      }
    }
  }

  @AutolockWrite
  public void filter (VectorPredicate<D> predicate) {

    Iterator<D> rosterIter = getRoster().iterator();

    while (rosterIter.hasNext()) {
      if (!predicate.isValid(rosterIter.next())) {
        rosterIter.remove();
      }
    }
  }

  @AutolockRead
  public synchronized D head () {

    if (getRoster().isEmpty()) {
      return null;
    }

    return getRoster().get(0);
  }

  @AutolockRead
  public synchronized List<D> asList () {

    return Collections.unmodifiableList(getRoster());
  }

  @AutolockRead
  public synchronized Iterator<D> iterator () {

    return Collections.unmodifiableList(getRoster()).iterator();
  }
}
