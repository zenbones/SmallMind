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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.smallmind.nutsnbolts.util.SingleItemIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ORMDao;
import org.terracotta.annotations.AutolockRead;
import org.terracotta.annotations.AutolockWrite;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public class ByKeySingularVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  private DurableKey<I, D> durableKey;

  public ByKeySingularVector (DurableKey<I, D> durableKey, int timeToLiveSeconds) {

    super(null, 1, timeToLiveSeconds, false);

    this.durableKey = durableKey;
  }

  private ORMDao<I, D> getORMDao () {

    ORMDao<I, D> ormDao;

    if ((ormDao = DaoManager.get(durableKey.getDurableClass())) == null) {
      throw new CacheOperationException("Unable to locate an implementation of ORMDao within DaoManager for the requested durable(%s)", durableKey.getDurableClass().getSimpleName());
    }

    return ormDao;
  }

  private D getDurable () {

    int equalsPos;

    if ((equalsPos = durableKey.getKey().indexOf('=')) < 0) {
      throw new CacheOperationException("Invalid durable key(%s)", durableKey);
    }

    return getORMDao().get(getORMDao().getIdFromString(durableKey.getKey().substring(equalsPos + 1)));
  }

  @AutolockRead
  public DurableVector<I, D> copy () {

    return new ByKeySingularVector<I, D>(durableKey, getTimeToLiveSeconds());
  }

  public boolean isSingular () {

    return true;
  }

  @AutolockWrite
  public synchronized void add (D durable) {

    if (!getDurable().equals(durable)) {
      durableKey = new DurableKey<I, D>(durableKey.getDurableClass(), durable.getId());
    }
  }

  public void remove (D durable) {

    throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
  }

  @AutolockRead
  public synchronized D head () {

    return getDurable();
  }

  @AutolockRead
  public synchronized List<D> asList () {

    return Collections.singletonList(getDurable());
  }

  @AutolockRead
  public synchronized Iterator<D> iterator () {

    return new SingleItemIterator<D>(getDurable());
  }
}
