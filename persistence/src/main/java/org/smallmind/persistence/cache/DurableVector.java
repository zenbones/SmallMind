/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

import java.util.Comparator;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorPredicate;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class DurableVector<I extends Comparable<I>, D extends Durable<I>> implements Iterable<D> {

   private Comparator<D> comparator;
   private boolean ordered;
   private long creationTime;
   private long timeToLive;
   private int maxSize;

   public DurableVector (Comparator<D> comparator, int maxSize, long timeToLive, boolean ordered) {

      this.comparator = comparator;
      this.maxSize = maxSize;
      this.timeToLive = timeToLive;
      this.ordered = ordered;

      creationTime = System.currentTimeMillis();
   }

   public abstract DurableVector<I, D> copy ();

   public abstract boolean isSingular ();

   public Comparator<D> getComparator () {

      return comparator;
   }

   public int getMaxSize () {

      return maxSize;
   }

   public long getTimeToLive () {

      return timeToLive;
   }

   public boolean isOrdered () {

      return ordered;
   }

   public boolean isAlive () {

      return (timeToLive <= 0) || (System.currentTimeMillis() - creationTime <= timeToLive);
   }

   public abstract void add (D durable);

   public abstract void remove (D durable);

   public abstract void removeId (I id);

   public abstract void filter (VectorPredicate<D> predicate);

   public abstract D head ();

   public abstract List<D> asList ();
}