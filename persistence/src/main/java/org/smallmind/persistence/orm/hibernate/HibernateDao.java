/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.persistence.orm.hibernate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.sql.SqlType;

public abstract class HibernateDao<I extends Serializable & Comparable<I>, D extends HibernateDurable<I, D>> extends ORMDao<I, D, SessionFactory, Session> {

  public HibernateDao (HibernateProxySession proxySession) {

    this(proxySession, null);
  }

  public HibernateDao (HibernateProxySession proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);
  }

  @Override
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

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().get(durableClass, id));
  }

  @Override
  public List<D> list () {

    return Collections.checkedList(getSession().getNativeSession().createCriteria(getManagedClass()).list(), getManagedClass());
  }

  @Override
  public List<D> list (int maxResults) {

    return list(maxResults, maxResults);
  }

  public List<D> list (int maxResults, int fetchSize) {

    return Collections.checkedList(getSession().getNativeSession().createCriteria(getManagedClass()).setMaxResults(maxResults).setFetchSize(fetchSize).list(), getManagedClass());
  }

  @Override
  public List<D> list (I greaterThan, int maxResults) {

    return list(greaterThan, maxResults, maxResults);
  }

  public List<D> list (final I greaterThan, final int maxResults, final int fetchSize) {

    return listByCriteria(new CriteriaDetails() {

      @Override
      public Criteria completeCriteria (Criteria criteria) {

        return criteria.add(Restrictions.gt("id", greaterThan)).addOrder(Order.asc("id")).setMaxResults(maxResults).setFetchSize(fetchSize);
      }
    });
  }

  @Override
  public List<D> list (Collection<I> idCollection) {

    if ((idCollection == null) || idCollection.isEmpty()) {

      return Collections.emptyList();
    } else {

      return listByCriteria(new CriteriaDetails() {

        @Override
        public Criteria completeCriteria (Criteria criteria) {

          return criteria.add(Restrictions.in("id", idCollection));
        }
      });
    }
  }

  @Override
  public Iterable<D> scroll () {

    return new ScrollIterable<D>(getSession().getNativeSession().createCriteria(getManagedClass()).scroll(ScrollMode.FORWARD_ONLY), getManagedClass());
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return new ScrollIterable<>(getSession().getNativeSession().createCriteria(getManagedClass()).setFetchSize(fetchSize).scroll(ScrollMode.FORWARD_ONLY), getManagedClass());
  }

  @Override
  public Iterable<D> scrollById (final I greaterThan, final int fetchSize) {

    return scrollByCriteria(new CriteriaDetails() {

      @Override
      public Criteria completeCriteria (Criteria criteria) {

        return criteria.add(Restrictions.gt("id", greaterThan)).addOrder(Order.asc("id")).setFetchSize(fetchSize);
      }
    });
  }

  @Override
  public long size () {

    return findByCriteria(Long.class, new CriteriaDetails() {

      @Override
      public Criteria completeCriteria (Criteria criteria) {

        return criteria.setProjection(Projections.rowCount());
      }
    });
  }

  public I lastId () {

    return findByCriteria(getIdClass(), new CriteriaDetails() {

      @Override
      public Criteria completeCriteria (Criteria criteria) {

        return criteria.setProjection(Projections.max("id"));
      }
    });
  }

  @Override
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

  @Override
  public void delete (Class<D> durableClass, D durable) {

    if (durable != null) {

      VectoredDao<I, D> vectoredDao = getVectoredDao();

      if (!getSession().getNativeSession().contains(durable)) {

        D persistedDurable;

        if ((persistedDurable = getSession().getNativeSession().get(durableClass, durable.getId())) != null) {
          getSession().getNativeSession().delete(persistedDurable);
          getSession().flush();
        }
      } else {
        getSession().getNativeSession().delete(durable);
        getSession().flush();
      }

      if (vectoredDao != null) {
        vectoredDao.delete(durableClass, durable);
      }
    }
  }

  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("Hibernate has no explicit detached state");
  }

  public int executeWithSQLQuery (NativeQueryDetails nativeQueryDetails) {

    return constructSQLQuery(nativeQueryDetails).executeUpdate();
  }

  public D findBySQLQuery (NativeQueryDetails nativeQueryDetails) {

    return getManagedClass().cast(constructSQLQuery(nativeQueryDetails).addEntity(getManagedClass()).uniqueResult());
  }

  public <T> T findBySQLQuery (Class<T> returnType, NativeQueryDetails nativeQueryDetails) {

    SQLQuery sqlQuery;

    sqlQuery = constructSQLQuery(nativeQueryDetails);

    if (Durable.class.isAssignableFrom(returnType)) {

      return returnType.cast(sqlQuery.addEntity(returnType).uniqueResult());
    } else if (!SqlType.isKnownType(returnType)) {

      return returnType.cast(sqlQuery.setResultTransformer(Transformers.aliasToBean(returnType)).uniqueResult());
    } else {

      Object obj;

      if ((obj = sqlQuery.uniqueResult()) != null) {

        return returnType.cast(obj);
      }

      return null;
    }
  }

  public List<D> listBySQLQuery (NativeQueryDetails nativeQueryDetails) {

    return Collections.checkedList(constructSQLQuery(nativeQueryDetails).addEntity(getManagedClass()).list(), getManagedClass());
  }

  public <T> List<T> listBySQLQuery (Class<T> returnType, NativeQueryDetails nativeQueryDetails) {

    SQLQuery sqlQuery;

    sqlQuery = constructSQLQuery(nativeQueryDetails);

    if (Durable.class.isAssignableFrom(returnType)) {

      return Collections.checkedList(sqlQuery.addEntity(returnType).list(), returnType);
    } else if (!SqlType.isKnownType(returnType)) {

      return Collections.checkedList(sqlQuery.setResultTransformer(Transformers.aliasToBean(returnType)).list(), returnType);
    } else {

      LinkedList<T> returnList = new LinkedList<T>();

      for (Object obj : sqlQuery.list()) {
        returnList.add(returnType.cast(obj));
      }

      return returnList;
    }
  }

  public Iterable<D> scrollBySQLQuery (NativeQueryDetails nativeQueryDetails) {

    return new ScrollIterable<>(constructSQLQuery(nativeQueryDetails).addEntity(getManagedClass()).scroll(ScrollMode.FORWARD_ONLY), getManagedClass());
  }

  public int executeWithQuery (QueryDetails queryDetails) {

    return constructQuery(queryDetails).executeUpdate();
  }

  public <T> T findByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return returnType.cast(constructQuery(queryDetails).uniqueResult());
  }

  public D findByQuery (QueryDetails queryDetails) {

    return getManagedClass().cast(constructQuery(queryDetails).uniqueResult());
  }

  public <T> List<T> listByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).list(), returnType);
  }

  public List<D> listByQuery (QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).list(), getManagedClass());
  }

  public <T> Iterable<T> scrollByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return new ScrollIterable<>(constructQuery(queryDetails).scroll(ScrollMode.FORWARD_ONLY), returnType);
  }

  public Iterable<D> scrollByQuery (QueryDetails queryDetails) {

    return new ScrollIterable<>(constructQuery(queryDetails).scroll(ScrollMode.FORWARD_ONLY), getManagedClass());
  }

  public <T> T findByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

    return returnType.cast(constructCriteria(criteriaDetails).uniqueResult());
  }

  public D findByCriteria (CriteriaDetails criteriaDetails) {

    return getManagedClass().cast(constructCriteria(criteriaDetails).uniqueResult());
  }

  public <T> List<T> listByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

    return Collections.checkedList(constructCriteria(criteriaDetails).list(), returnType);
  }

  public List<D> listByCriteria (CriteriaDetails criteriaDetails) {

    return Collections.checkedList(constructCriteria(criteriaDetails).list(), getManagedClass());
  }

  public <T> Iterable<T> scrollByCriteria (Class<T> returnType, CriteriaDetails criteriaDetails) {

    return new ScrollIterable<>(constructCriteria(criteriaDetails).scroll(ScrollMode.FORWARD_ONLY), returnType);
  }

  public Iterable<D> scrollByCriteria (CriteriaDetails criteriaDetails) {

    return new ScrollIterable<>(constructCriteria(criteriaDetails).scroll(ScrollMode.FORWARD_ONLY), getManagedClass());
  }

  public SQLQuery constructSQLQuery (NativeQueryDetails nativeQueryDetails) {

    return nativeQueryDetails.completeNativeQuery(getSession().getNativeSession().createSQLQuery(nativeQueryDetails.getNativeQueryString()).setCacheable(true));
  }

  public Query constructQuery (QueryDetails queryDetails) {

    return queryDetails.completeQuery(getSession().getNativeSession().createQuery(queryDetails.getQueryString()).setCacheable(true));
  }

  private Criteria constructCriteria (CriteriaDetails criteriaDetails) {

    Criteria criteria;

    criteria = (criteriaDetails.getAlias() == null) ? getSession().getNativeSession().createCriteria(criteriaDetails.getCriteriaClass(getManagedClass())) : getSession().getNativeSession().createCriteria(criteriaDetails.getCriteriaClass(getManagedClass()), criteriaDetails.getAlias());

    return criteriaDetails.completeCriteria(criteria).setCacheable(true);
  }

  public DetachedCriteria detachCriteria () {

    return DetachedCriteria.forClass(getManagedClass());
  }
}
