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
package org.smallmind.persistence.orm.jpa;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.persistence.Query;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.VectorAwareORMDao;

public abstract class JPADao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends VectorAwareORMDao<I, D> {

  private JPAProxySession proxySession;

  public JPADao (JPAProxySession proxySession) {

    this(proxySession, null);
  }

  public JPADao (JPAProxySession proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);

    this.proxySession = proxySession;
  }

  public void register () {

    DaoManager.register(getManagedClass(), this);
  }

  public String getDataSource () {

    return proxySession.getDataSource();
  }

  public ProxySession getSession () {

    return proxySession;
  }

  public I getId (D durable) {

    return durable.getId();
  }

  public D get (I id) {

    return get(getManagedClass(), id);
  }

  public D get (Class<D> durableClass, I id) {

    VectoredDao<I, D> vectoredDao;
    Object persistedObject;

    if ((vectoredDao = getVectoredDao()) == null) {
      if ((persistedObject = proxySession.getEntityManager().find(durableClass, id)) != null) {

        return durableClass.cast(persistedObject);
      }
    }
    else {

      D durable;

      if ((durable = vectoredDao.get(durableClass, id)) != null) {

        return durable;
      }

      if ((persistedObject = proxySession.getEntityManager().find(durableClass, id)) != null) {
        durable = durableClass.cast(persistedObject);

        return vectoredDao.persist(durableClass, durable, PersistenceMode.SOFT);
      }
    }

    return null;
  }

  public D persist (D durable) {

    return persist(getManagedClass(), durable);
  }

  public D persist (Class<D> durableClass, D durable) {

    D persistentDurable;
    VectoredDao<I, D> vectoredDao = getVectoredDao();

    if (proxySession.getEntityManager().contains(durable)) {
      persistentDurable = durable;
    }
    else {
      persistentDurable = getManagedClass().cast(proxySession.getEntityManager().merge(durable));
      proxySession.flush();
    }

    if (vectoredDao != null) {

      return vectoredDao.persist(durableClass, persistentDurable, PersistenceMode.HARD);
    }

    return persistentDurable;
  }

  public void delete (D durable) {

    delete(getManagedClass(), durable);
  }

  public void delete (Class<D> durableClass, D durable) {

    VectoredDao<I, D> vectoredDao = getVectoredDao();

    if (!proxySession.getEntityManager().contains(durable)) {
      proxySession.getEntityManager().remove(proxySession.getEntityManager().find(durable.getClass(), durable.getId()));
    }
    else {
      proxySession.getEntityManager().remove(durable);
    }

    proxySession.flush();

    if (vectoredDao != null) {
      vectoredDao.delete(durableClass, durable);
    }
  }

  public D detach (D object) {

    throw new UnsupportedOperationException("JPA has no explicit detached state");
  }

  public int executeWithQuery (QueryDetails queryDetails) {

    return constructQuery(queryDetails).executeUpdate();
  }

  public <T> T findByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return returnType.cast(constructQuery(queryDetails).getSingleResult());
  }

  public D findByQuery (QueryDetails queryDetails) {

    return getManagedClass().cast(constructQuery(queryDetails).getSingleResult());
  }

  public <T> List<T> listByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).getResultList(), returnType);
  }

  public List<D> listByQuery (QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).getResultList(), getManagedClass());
  }

  public Query constructQuery (QueryDetails queryDetails) {

    return queryDetails.completeQuery(proxySession.getEntityManager().createQuery(queryDetails.getQueryString()));
  }
}
