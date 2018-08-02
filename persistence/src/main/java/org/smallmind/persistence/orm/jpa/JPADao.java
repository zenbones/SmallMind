/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.persistence.orm.jpa;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;

public abstract class JPADao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends ORMDao<I, D, EntityManagerFactory, EntityManager> {

  public JPADao (JPAProxySession proxySession) {

    this(proxySession, null);
  }

  public JPADao (JPAProxySession proxySession, VectoredDao<I, D> vectoredDao) {

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

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().find(durableClass, id));
  }

  public D persist (Class<D> durableClass, D durable) {

    if (durable != null) {

      D persistentDurable;
      VectoredDao<I, D> vectoredDao = getVectoredDao();

      if (getSession().getNativeSession().contains(durable)) {
        persistentDurable = durable;
      } else {
        persistentDurable = getManagedClass().cast(getSession().getNativeSession().merge(durable));
        getSession().flush();
      }

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

      if (!getSession().getNativeSession().contains(durable)) {

        D persitentDurable;

        if ((persitentDurable = getSession().getNativeSession().find(durableClass, durable.getId())) != null) {
          getSession().getNativeSession().remove(persitentDurable);
          getSession().flush();
        }
      } else {
        getSession().getNativeSession().remove(durable);
        getSession().flush();
      }

      if (vectoredDao != null) {
        vectoredDao.delete(durableClass, durable);
      }
    }
  }

  public List<D> list () {

    return listByQuery(new QueryDetails() {

      @Override
      public String getQueryString () {

        return "select entity from " + getManagedClass().getSimpleName() + " entity";
      }

      @Override
      public Query completeQuery (Query query) {

        return query;
      }
    });
  }

  public List<D> list (final int fetchSize) {

    return listByQuery(new QueryDetails() {

      @Override
      public String getQueryString () {

        return "select entity from " + getManagedClass().getSimpleName() + " entity";
      }

      @Override
      public Query completeQuery (Query query) {

        return query.setMaxResults(fetchSize);
      }
    });
  }

  public List<D> list (final I greaterThan, final int fetchSize) {

    return listByQuery(new QueryDetails() {

      @Override
      public String getQueryString () {

        return "select entity from " + getManagedClass().getSimpleName() + " entity where entity.id > ?";
      }

      @Override
      public Query completeQuery (Query query) {

        return query.setParameter("id", greaterThan).setMaxResults(fetchSize);
      }
    });
  }

  public Iterable<D> scroll () {

    throw new UnsupportedOperationException();
  }

  public Iterable<D> scroll (int fetchSize) {

    throw new UnsupportedOperationException();
  }

  public Iterable<D> scrollById (I greaterThan, int fetchSize) {

    throw new UnsupportedOperationException();
  }

  public long size () {

    throw new UnsupportedOperationException();
  }

  public D detach (D object) {

    throw new UnsupportedOperationException("JPA has no explicit detached state");
  }

  public int deleteWithCriteria (CriteriaDeleteDetails<D> criteriaDeleteDetails) {

    return constructCriteriaDelete(getManagedClass(), criteriaDeleteDetails).executeUpdate();
  }

  public <T> int deleteWithCriteria (Class<T> criteriaType, CriteriaDeleteDetails<T> criteriaDeleteDetails) {

    return constructCriteriaDelete(criteriaType, criteriaDeleteDetails).executeUpdate();
  }

  public int executeWithQuery (QueryDetails queryDetails) {

    return constructQuery(queryDetails).executeUpdate();
  }

  public int executeWithCriteria (CriteriaUpdateDetails<D> criteriaUpdateDetails) {

    return constructCriteriaUpdate(getManagedClass(), criteriaUpdateDetails).executeUpdate();
  }

  public <T> int executeWithCriteria (Class<T> criteriaType, CriteriaUpdateDetails<T> criteriaUpdateDetails) {

    return constructCriteriaUpdate(criteriaType, criteriaUpdateDetails).executeUpdate();
  }

  public <T> T findByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return returnType.cast(constructQuery(queryDetails).getSingleResult());
  }

  public D findByQuery (QueryDetails queryDetails) {

    return getManagedClass().cast(constructQuery(queryDetails).getSingleResult());
  }

  public <T> T findByCriteria (Class<T> returnType, CriteriaQueryDetails<T> criteriaQueryDetails) {

    return returnType.cast(constructCriteriaQuery(returnType, criteriaQueryDetails).getSingleResult());
  }

  public D findByCriteria (CriteriaQueryDetails<D> criteriaQueryDetails) {

    return getManagedClass().cast(constructCriteriaQuery(getManagedClass(), criteriaQueryDetails).getSingleResult());
  }

  public <T> List<T> listByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).getResultList(), returnType);
  }

  public List<D> listByQuery (QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).getResultList(), getManagedClass());
  }

  public <T> List<T> listByCriteria (Class<T> returnType, CriteriaQueryDetails<T> criteriaQueryDetails) {

    return Collections.checkedList(constructCriteriaQuery(returnType, criteriaQueryDetails).getResultList(), returnType);
  }

  public List<D> listByCriteria (CriteriaQueryDetails<D> criteriaQueryDetails) {

    return Collections.checkedList(constructCriteriaQuery(getManagedClass(), criteriaQueryDetails).getResultList(), getManagedClass());
  }

  public Query constructQuery (QueryDetails queryDetails) {

    return queryDetails.completeQuery(getSession().getNativeSession().createQuery(queryDetails.getQueryString()));
  }

  public <T> Query constructCriteriaQuery (Class<T> criteriaClass, CriteriaQueryDetails<T> criteriaQueryDetails) {

    return getSession().getNativeSession().createQuery(criteriaQueryDetails.completeCriteria(criteriaClass, getSession().getNativeSession().getCriteriaBuilder()));
  }

  public <T> Query constructCriteriaUpdate (Class<T> criteriaClass, CriteriaUpdateDetails<T> criteriaUpdateDetails) {

    return getSession().getNativeSession().createQuery(criteriaUpdateDetails.completeCriteria(criteriaClass, getSession().getNativeSession().getCriteriaBuilder()));
  }

  public <T> Query constructCriteriaDelete (Class<T> criteriaClass, CriteriaDeleteDetails<T> criteriaDeleteDetails) {

    return getSession().getNativeSession().createQuery(criteriaDeleteDetails.completeCriteria(criteriaClass, getSession().getNativeSession().getCriteriaBuilder()));
  }
}
