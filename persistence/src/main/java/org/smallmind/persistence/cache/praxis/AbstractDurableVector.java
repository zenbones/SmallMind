/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public abstract class AbstractDurableVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  public AbstractDurableVector (Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    super(comparator, maxSize, timeToLiveSeconds, ordered);
  }

  public abstract Roster<D> getRoster ();

  public abstract DurableVector<I, D> copy ();

  public boolean isSingular () {

    return false;
  }

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

  public synchronized D head () {

    if (getRoster().isEmpty()) {
      return null;
    }

    return getRoster().get(0);
  }
}
