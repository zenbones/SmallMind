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
package org.smallmind.persistence.orm.jdo;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jdo.Query;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.CacheAwareORMDao;
import org.smallmind.persistence.orm.ProxySession;

public abstract class JDODao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends CacheAwareORMDao<I, D> {

  private JDOProxySession proxySession;

  public JDODao (JDOProxySession proxySession) {

    this(proxySession, null);
  }

  public JDODao (JDOProxySession proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);

    this.proxySession = proxySession;
  }

  public String getDataSource () {

    return proxySession.getDataSource();
  }

  public ProxySession getSession () {

    return proxySession;
  }

  public I getId (D object) {

    return getIdClass().cast(proxySession.getPersistenceManager().getObjectId(object));
  }

  public D get (I id) {

    return get(getManagedClass(), id);
  }

  public D get (Class<D> durableClass, I id) {

    D durable;
    Object persistedObject;
    VectoredDao<I, D> vectoredDao = getVectoredDao();

    if (vectoredDao != null) {
      if ((durable = vectoredDao.get(durableClass, id)) != null) {

        return durable;
      }
    }

    if ((persistedObject = proxySession.getPersistenceManager().getObjectId(id)) != null) {
      durable = durableClass.cast(persistedObject);

      if (vectoredDao != null) {

        return vectoredDao.persist(durableClass, durable);
      }

      return durable;
    }

    return null;
  }

  public List<D> list () {

    LinkedList<D> instanceList;
    Iterator instanceIter;

    instanceIter = proxySession.getPersistenceManager().getExtent(getManagedClass()).iterator();
    instanceList = new LinkedList<D>();
    while (instanceIter.hasNext()) {
      instanceList.add(getManagedClass().cast(instanceIter.next()));
    }

    return instanceList;
  }

  public Iterable<D> scroll () {

    return new IterableIterator<D>(proxySession.getPersistenceManager().getExtent(getManagedClass()).iterator());
  }

  public D detach (D object) {

    return getManagedClass().cast(proxySession.getPersistenceManager().detachCopy(object));
  }

  public D persist (D durable) {

    return persist(getManagedClass(), durable);
  }

  public D persist (Class<D> durableClass, D durable) {

    D persistentDurable;
    VectoredDao<I, D> vectoredDao = getVectoredDao();

    persistentDurable = durableClass.cast(proxySession.getPersistenceManager().makePersistent(durable));

    if (vectoredDao != null) {

      return vectoredDao.persist(durableClass, persistentDurable);
    }

    return persistentDurable;
  }

  public void delete (D durable) {

    delete(getManagedClass(), durable);
  }

  public void delete (Class<D> durableClass, D durable) {

    VectoredDao<I, D> vectoredDao = getVectoredDao();

    proxySession.getPersistenceManager().deletePersistent(durable);

    if (vectoredDao != null) {
      vectoredDao.delete(durableClass, durable);
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

    query = proxySession.getPersistenceManager().newQuery(queryDetails.getQuery());
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
