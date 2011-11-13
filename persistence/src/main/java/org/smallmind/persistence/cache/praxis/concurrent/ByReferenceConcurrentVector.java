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
package org.smallmind.persistence.cache.praxis.concurrent;

import java.io.Serializable;
import java.util.Comparator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.praxis.AbstractDurableVector;
import org.smallmind.persistence.cache.praxis.concurrent.util.ConcurrentRoster;
import org.terracotta.annotations.AutolockRead;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class ByReferenceConcurrentVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends AbstractDurableVector<I, D> {

  private ConcurrentRoster<D> roster;

  public ByReferenceConcurrentVector (ConcurrentRoster<D> roster, Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

    super(comparator, maxSize, timeToLive, ordered);

    this.roster = roster;
    if (maxSize > 0) {
      while (roster.size() > maxSize) {
        roster.removeLast();
      }
    }
  }

  @Override
  public ConcurrentRoster<D> getRoster () {

    return roster;
  }

  @AutolockRead
  public DurableVector<I, D> copy () {

    return new ByReferenceConcurrentVector<I, D>(new ConcurrentRoster<D>(roster), getComparator(), getMaxSize(), getTimeToLiveMilliseconds(), isOrdered());
  }
}
