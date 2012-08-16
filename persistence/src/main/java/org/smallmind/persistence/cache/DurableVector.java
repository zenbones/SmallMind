/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.persistence.cache;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class DurableVector<I extends Comparable<I>, D extends Durable<I>> implements Serializable, Iterable<D> {

  private Comparator<D> comparator;
  private boolean ordered;
  private long creationTimeMilliseconds;
  private int timeToLiveSeconds;
  private int maxSize;

  public DurableVector (Comparator<D> comparator, int maxSize, int timeToLiveSeconds, boolean ordered) {

    this.comparator = comparator;
    this.maxSize = maxSize;
    this.timeToLiveSeconds = timeToLiveSeconds;
    this.ordered = ordered;

    creationTimeMilliseconds = System.currentTimeMillis();
  }

  public abstract DurableVector<I, D> copy ();

  public abstract boolean isSingular ();

  public abstract boolean add (D durable);

  public abstract boolean remove (D durable);

  public abstract D head ();

  public abstract List<D> asList ();

  public List<D> prefetch () {

    return asList();
  }

  public Comparator<D> getComparator () {

    return comparator;
  }

  public int getMaxSize () {

    return maxSize;
  }

  public int getTimeToLiveSeconds () {

    return timeToLiveSeconds;
  }

  public boolean isOrdered () {

    return ordered;
  }

  public boolean isAlive () {

    return (timeToLiveSeconds <= 0) || ((System.currentTimeMillis() - creationTimeMilliseconds) / 1000 <= timeToLiveSeconds);
  }
}