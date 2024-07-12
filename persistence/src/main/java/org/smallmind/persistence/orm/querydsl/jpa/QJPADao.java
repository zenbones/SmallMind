/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.persistence.orm.querydsl.jpa;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAUpdateClause;
import org.smallmind.nutsnbolts.util.EmptyIterable;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.ProxySession;
import org.smallmind.persistence.orm.jpa.JPADurable;

public class QJPADao<I extends Serializable & Comparable<I>, D extends JPADurable<I, D>> extends ORMDao<I, D, EntityManagerFactory, EntityManager> {

  public QJPADao (ProxySession<EntityManagerFactory, EntityManager> proxySession) {

    super(proxySession, null);
  }

  public QJPADao (ProxySession<EntityManagerFactory, EntityManager> proxySession, VectoredDao<I, D> vectoredDao) {

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

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().find(durableClass, id));
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

        Object persistedState;

        if ((persistedState = getSession().getNativeSession().find(durable.getClass(), durable.getId())) != null) {
          getSession().getNativeSession().remove(persistedState);
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

  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("JPA has no explicit detached state");
  }

  @Override
  public long size () {

    return countByQuery(new JPAQueryDetails<D>() {

      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  @Override
  public List<D> list () {

    return listByQuery(new JPAQueryDetails<D>() {

      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  @Override
  public List<D> list (int fetchSize) {

    return listByQuery(new JPAQueryDetails<D>() {

      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity")).limit(fetchSize);
      }
    });
  }

  @Override
  public List<D> list (I greaterThan, int fetchSize) {

    return listByQuery(new JPAQueryDetails<D>() {

      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        PathBuilder<D> entityPath = new PathBuilder<>(getManagedClass(), "entity");
        Path<D> idPath = Expressions.path(getManagedClass(), entityPath, "id");

        return query.from(entityPath).where(Expressions.predicate(Ops.GT, idPath, Expressions.constant(greaterThan))).orderBy(new OrderSpecifier<D>(Order.ASC, idPath)).limit(fetchSize);
      }
    });
  }

  @Override
  public List<D> list (Collection<I> idCollection) {

    if ((idCollection == null) || idCollection.isEmpty()) {

      return Collections.emptyList();
    } else {

      return listByQuery(new JPAQueryDetails<D>() {

        @Override
        public JPAQuery<D> completeQuery (JPAQuery<D> query) {

          PathBuilder<D> entityPath = new PathBuilder<>(getManagedClass(), "entity");
          Path<D> idPath = Expressions.path(getManagedClass(), entityPath, "id");
          Iterator<I> idIterator = idCollection.iterator();
          Expression<?> collectionExpression = Expressions.collectionOperation(getIdClass(), Ops.SINGLETON, Expressions.constant(idIterator.next()));

          while (idIterator.hasNext()) {
            collectionExpression = Expressions.collectionOperation(getIdClass(), Ops.LIST, collectionExpression, Expressions.constant(idIterator.next()));
          }

          return query.from(entityPath).where(Expressions.predicate(Ops.IN, idPath, collectionExpression));
        }
      });
    }
  }

  @Override
  public Iterable<D> scroll () {

    return scrollByQuery(new JPAQueryDetails<D>() {

      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return scrollByQuery(new JPAQueryDetails<D>() {

      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity")).limit(fetchSize);
      }
    });
  }

  @Override
  public Iterable<D> scrollById (I greaterThan, int fetchSize) {

    return scrollByQuery(new JPAQueryDetails<D>() {

      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        PathBuilder<D> entityPath = new PathBuilder<>(getManagedClass(), "entity");
        Path<D> idPath = Expressions.path(getManagedClass(), entityPath, "id");

        return query.from(entityPath).where(Expressions.predicate(Ops.GT, idPath, Expressions.constant(greaterThan))).orderBy(new OrderSpecifier<D>(Order.ASC, idPath)).limit(fetchSize);
      }
    });
  }

  public <T> long countByQuery (JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? 0 : constructedQuery.fetchCount();
  }

  public D findByQuery (JPAQueryDetails<D> queryDetails) {

    JPAQuery<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : getManagedClass().cast(constructedQuery.fetchOne());
  }

  public <T> T findByQuery (Class<T> returnType, JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : returnType.cast(constructedQuery.fetchOne());
  }

  public List<D> listByQuery (JPAQueryDetails<D> queryDetails) {

    JPAQuery<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? Collections.emptyList() : Collections.checkedList(constructedQuery.fetch(), getManagedClass());
  }

  public <T> List<T> listByQuery (Class<T> returnType, JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? Collections.emptyList() : Collections.checkedList(constructedQuery.fetch(), returnType);
  }

  public <T> Iterable<T> scrollByQuery (JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? new EmptyIterable<>() : new IterableIterator<>(constructedQuery.iterate());
  }

  public Long updateWithQuery (JPAUpdateDetails<D> updateDetails) {

    JPAUpdateClause updateClause;

    return ((updateClause = constructUpdate(updateDetails)) == null) ? 0 : updateClause.execute();
  }

  public Long deleteWithQuery (JPADeleteDetails<D> deleteDetails) {

    JPADeleteClause deleteClause;

    return ((deleteClause = constructDelete(deleteDetails)) == null) ? 0 : deleteClause.execute();
  }

  private <T> JPAQuery<T> constructQuery (JPAQueryDetails<T> queryDetails) {

    EntityManager entityManager = getSession().getNativeSession();
    JPAQuery<T> query = new JPAQuery<>(entityManager);

    if (queryDetails.getEntityGraphSetting() != null) {
      query.setHint(queryDetails.getEntityGraphSetting().getHint().getKey(), entityManager.createEntityGraph(queryDetails.getEntityGraphSetting().getName()));
    }

    return queryDetails.completeQuery(query);
  }

  private <T> JPAUpdateClause constructUpdate (JPAUpdateDetails<T> updateDetails) {

    JPAUpdateClause updateClause = new JPAUpdateClause(getSession().getNativeSession(), updateDetails.getEntityPath());

    return updateDetails.completeUpdate(updateClause);
  }

  private <T> JPADeleteClause constructDelete (JPADeleteDetails<T> deleteDetails) {

    JPADeleteClause deleteClause = new JPADeleteClause(getSession().getNativeSession(), deleteDetails.getEntityPath());

    return deleteDetails.completeDelete(deleteClause);
  }
}
