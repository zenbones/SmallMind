/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.smallmind.nutsnbolts.util.SingleItemIterable;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheOperationException;
import org.smallmind.persistence.cache.DurableKey;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;

public class ByKeySingularVector<I extends Serializable & Comparable<I>, D extends Durable<I>> extends DurableVector<I, D> {

  private transient volatile ORMDao<I, D, ?, ?> ormDao;

  private DurableKey<I, D> durableKey;

  public ByKeySingularVector (DurableKey<I, D> durableKey, int timeToLiveSeconds) {

    super(null, 1, timeToLiveSeconds, false);

    this.durableKey = durableKey;
  }

  private ORMDao<I, D, ?, ?> getORMDao () {

    if (ormDao == null) {
      if ((ormDao = OrmDaoManager.get(durableKey.getDurableClass())) == null) {
        throw new CacheOperationException("Unable to locate an implementation of ORMDao within DaoManager for the requested durable(%s)", durableKey.getDurableClass().getSimpleName());
      }
    }

    return ormDao;
  }

  private D getDurable () {

    D durable;
    ORMDao<I, D, ?, ?> ormDao;

    if ((durable = (ormDao = getORMDao()).get(ormDao.getIdFromString(durableKey.getIdAsString()))) == null) {
      throw new CacheOperationException("Unable to locate the requested durable(%s) instance(%s)", durableKey.getDurableClass().getSimpleName(), durableKey.getIdAsString());
    }

    return durable;
  }

  public DurableVector<I, D> copy () {

    return new ByKeySingularVector<>(durableKey, getTimeToLiveSeconds());
  }

  public boolean isSingular () {

    return true;
  }

  public synchronized boolean add (D durable) {

    if (!getDurable().equals(durable)) {
      durableKey = new DurableKey<>(durableKey.getDurableClass(), durable.getId());

      return true;
    }

    return false;
  }

  public boolean remove (D durable) {

    throw new UnsupportedOperationException("Attempted removal from a 'singular' vector");
  }

  public synchronized D head () {

    return getDurable();
  }

  public synchronized List<D> asBestEffortLazyList () {

    return Collections.singletonList(getDurable());
  }

  public synchronized Iterator<D> iterator () {

    return new SingleItemIterable<>(getDurable()).iterator();
  }
}
