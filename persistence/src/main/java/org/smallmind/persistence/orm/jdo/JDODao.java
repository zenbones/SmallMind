/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.persistence.orm.jdo;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;

public abstract class JDODao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends ORMDao<I, D, PersistenceManagerFactory, PersistenceManager> {

  public JDODao (JDOProxySession proxySession) {

    this(proxySession, null);
  }

  public JDODao (JDOProxySession proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);
  }

  public D get (Class<D> durableClass, I id) {

    if (id != null) {

      VectoredDao<I, D> vectoredDao;
      D durable;

      if ((vectoredDao = getVectoredDao()) == null) {
        if ((durable = acquire(durableClass, id)) != null) {

          return durable;
        }
      } else {
        if ((durable = vectoredDao.get(durableClass, id)) != null) {

          return durable;
        }

        if ((durable = acquire(durableClass, id)) != null) {

          return vectoredDao.persist(durableClass, durable, UpdateMode.SOFT);
        }
      }
    }

    return null;
  }

  @Override
  public D acquire (Class<D> durableClass, I id) {

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().getObjectById(durableClass, id));
  }

  public List<D> list () {

    LinkedList<D> instanceList;
    Iterator instanceIter;

    instanceIter = getSession().getNativeSession().getExtent(getManagedClass()).iterator();
    instanceList = new LinkedList<D>();
    while (instanceIter.hasNext()) {
      instanceList.add(getManagedClass().cast(instanceIter.next()));
    }

    return instanceList;
  }

  public Iterable<D> scroll () {

    return new IterableIterator<D>(getSession().getNativeSession().getExtent(getManagedClass()).iterator());
  }

  public D detach (D durable) {

    return (durable == null) ? null : getManagedClass().cast(getSession().getNativeSession().detachCopy(durable));
  }

  public D persist (Class<D> durableClass, D durable) {

    if (durable != null) {

      D persistentDurable;
      VectoredDao<I, D> vectoredDao = getVectoredDao();

      persistentDurable = durableClass.cast(getSession().getNativeSession().makePersistent(durable));

      if (vectoredDao != null) {

        return vectoredDao.persist(durableClass, persistentDurable, UpdateMode.HARD);
      }

      return persistentDurable;
    }

    return null;
  }

  public void delete (Class<D> durableClass, D durable) {

    if (durable != null) {

      VectoredDao<I, D> vectoredDao = getVectoredDao();

      getSession().getNativeSession().deletePersistent(durable);

      if (vectoredDao != null) {
        vectoredDao.delete(durableClass, durable);
      }
    }
  }

  public D find (QueryDetails queryDetails) {

    Query query;

    query = constructQuery(queryDetails);
    query.setUnique(true);

    return getManagedClass().cast(query.executeWithMap(queryDetails.getParameterMap()));
  }

  public List<D> list (QueryDetails queryDetails) {

    return Collections.checkedList((List<D>)constructQuery(queryDetails).executeWithMap(queryDetails.getParameterMap()), getManagedClass());
  }

  private Query constructQuery (QueryDetails queryDetails) {

    Query query;
    Class[] importClasses;

    query = getSession().getNativeSession().newQuery(queryDetails.getQuery());
    query.setIgnoreCache(queryDetails.getIgnoreCache());

    if ((importClasses = queryDetails.getImports()) != null) {
      if (importClasses.length > 0) {

        StringBuilder importBuilder;

        importBuilder = new StringBuilder("import ");
        for (int count = 0; count < importClasses.length; count++) {
          if (count > 0) {
            importBuilder.append("; ");
          }

          importBuilder.append(importClasses[count].getCanonicalName());
        }

        query.declareImports(importBuilder.toString());
      }
    }

    return query;
  }
}
