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
package org.smallmind.persistence.orm.querydsl.hibernate;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.hibernate.HibernateDeleteClause;
import com.querydsl.jpa.hibernate.HibernateQuery;
import com.querydsl.jpa.hibernate.HibernateUpdateClause;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.hibernate.HibernateDurable;

public class QHibernateDao<I extends Serializable & Comparable<I>, D extends HibernateDurable<I, D>> extends ORMDao<I, D, SessionFactory, Session> {

  public QHibernateDao (ProxySession<SessionFactory, Session> proxySession) {

    super(proxySession, null);
  }

  public QHibernateDao (ProxySession<SessionFactory, Session> proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);
  }

  @Override
  public D get (Class<D> durableClass, I id) {

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

    return null;
  }

  @Override
  public D acquire (Class<D> durableClass, I id) {

    return durableClass.cast(getSession().getNativeSession().get(durableClass, id));
  }

  @Override
  public D persist (Class<D> durableClass, D durable) {

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

  @Override
  public void delete (Class<D> durableClass, D durable) {

    VectoredDao<I, D> vectoredDao = getVectoredDao();

    if (!getSession().getNativeSession().contains(durable)) {
      getSession().getNativeSession().delete(getSession().getNativeSession().load(durable.getClass(), durable.getId()));
    } else {
      getSession().getNativeSession().delete(durable);
    }

    getSession().flush();

    if (vectoredDao != null) {
      vectoredDao.delete(durableClass, durable);
    }
  }

  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("Hibernate has no explicit detached state");
  }

  @Override
  public long size () {

    return countByQuery(new HibernateQueryDetails<D>() {

      @Override
      public HibernateQuery<D> completeQuery (HibernateQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  @Override
  public List<D> list () {

    return listByQuery(new HibernateQueryDetails<D>() {

      @Override
      public HibernateQuery<D> completeQuery (HibernateQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  @Override
  public List<D> list (int fetchSize) {

    return listByQuery(new HibernateQueryDetails<D>() {

      @Override
      public HibernateQuery<D> completeQuery (HibernateQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity")).limit(fetchSize);
      }
    });
  }

  @Override
  public List<D> list (I greaterThan, int fetchSize) {

    return listByQuery(new HibernateQueryDetails<D>() {

      @Override
      public HibernateQuery<D> completeQuery (HibernateQuery<D> query) {

        PathBuilder<D> entityPath = new PathBuilder<>(getManagedClass(), "entity");
        Path<D> idPath = Expressions.path(getManagedClass(), entityPath, "id");

        return query.from(entityPath).where(Expressions.predicate(Ops.GT, idPath, Expressions.constant(greaterThan))).orderBy(new OrderSpecifier<D>(Order.ASC, idPath)).limit(fetchSize);
      }
    });
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return null;
  }

  @Override
  public Iterable<D> scrollById (I greaterThan, int fetchSize) {

    return null;
  }

  @Override
  public Iterable<D> scroll () {

    return null;
  }

  public long countByQuery (HibernateQueryDetails<D> queryDetails) {

    return constructQuery(queryDetails).fetchCount();
  }

  public <T> long countByQuery (Class<T> returnType, HibernateQueryDetails<T> queryDetails) {

    return constructQuery(queryDetails).fetchCount();
  }

  public D findByQuery (HibernateQueryDetails<D> queryDetails) {

    return getManagedClass().cast(constructQuery(queryDetails).fetchOne());
  }

  public <T> T findByQuery (Class<T> returnType, HibernateQueryDetails<T> queryDetails) {

    return returnType.cast(constructQuery(queryDetails).fetchOne());
  }

  public List<D> listByQuery (HibernateQueryDetails<D> queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).fetch(), getManagedClass());
  }

  public <T> List<T> listByQuery (Class<T> returnType, HibernateQueryDetails<T> queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).fetch(), returnType);
  }

  public Long updateWithQuery (HibernateUpdateDetails<D> updateDetails) {

    return constructUpdate(updateDetails).execute();
  }

  public Long deleteWithQuery (HibernateDeleteDetails<D> deleteDetails) {

    return constructDelete(deleteDetails).execute();
  }

  private <T> HibernateQuery<T> constructQuery (HibernateQueryDetails<T> queryDetails) {

    HibernateQuery<T> query = new HibernateQuery<T>(getSession().getNativeSession());

    return queryDetails.completeQuery(query);
  }

  private <T> HibernateUpdateClause constructUpdate (HibernateUpdateDetails<T> updateDetails) {

    HibernateUpdateClause updateClause = new HibernateUpdateClause(getSession().getNativeSession(), updateDetails.getEntityPath());

    return updateDetails.completeUpdate(updateClause);
  }

  private <T> HibernateDeleteClause constructDelete (HibernateDeleteDetails<T> deleteDetails) {

    HibernateDeleteClause deleteClause = new HibernateDeleteClause(getSession().getNativeSession(), deleteDetails.getEntityPath());

    return deleteDetails.completeDelete(deleteClause);
  }
}
