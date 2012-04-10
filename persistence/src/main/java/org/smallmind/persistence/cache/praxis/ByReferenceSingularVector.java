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
package org.smallmind.persistence.cache.praxis;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.smallmind.nutsnbolts.util.SingleItemIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;
import org.terracotta.annotations.AutolockRead;
import org.terracotta.annotations.AutolockWrite;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class ByReferenceSingularVector<I extends Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  private D durable;

  public ByReferenceSingularVector (D durable, int timeToLiveSeconds) {

    super(null, 1, timeToLiveSeconds, false);

    this.durable = durable;
  }

  @AutolockRead
  public DurableVector<I, D> copy () {

    return new ByReferenceSingularVector<I, D>(durable, getTimeToLiveSeconds());
  }

  public boolean isSingular () {

    return true;
  }

  @AutolockWrite
  public synchronized void add (D durable) {

    if (!this.durable.equals(durable)) {
      this.durable = durable;
    }
  }

  public void remove (D durable) {

    throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
  }

  @AutolockRead
  public synchronized D head () {

    return durable;
  }

  @AutolockRead
  public synchronized List<D> asList () {

    return Collections.singletonList(durable);
  }

  @AutolockRead
  public synchronized Iterator<D> iterator () {

    return new SingleItemIterator<D>(durable);
  }
}
