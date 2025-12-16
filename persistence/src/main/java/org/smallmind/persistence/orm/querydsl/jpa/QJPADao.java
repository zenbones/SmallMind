/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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

/**
 * JPA DAO implementation using QueryDSL for query construction, with optional vector cache integration.
 *
 * @param <I> identifier type
 * @param <D> durable entity type
 */
public class QJPADao<I extends Serializable & Comparable<I>, D extends JPADurable<I, D>> extends ORMDao<I, D, EntityManagerFactory, EntityManager> {

  /**
   * Constructs a QueryDSL JPA DAO without cache integration.
   *
   * @param proxySession the JPA proxy session
   */
  public QJPADao (ProxySession<EntityManagerFactory, EntityManager> proxySession) {

    super(proxySession, null);
  }

  /**
   * Constructs a QueryDSL JPA DAO with optional cache integration.
   *
   * @param proxySession the JPA proxy session
   * @param vectoredDao  cache-backed delegate used when caching is enabled
   */
  public QJPADao (ProxySession<EntityManagerFactory, EntityManager> proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);
  }

  /**
   * Retrieves a durable by id, consulting cache first when available.
   *
   * @param durableClass the entity class
   * @param id           the identifier to find
   * @return the durable instance or {@code null} if not found
   */
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

  /**
   * Fetches a durable directly from the EntityManager by id.
   *
   * @param durableClass entity class
   * @param id           identifier to locate
   * @return managed durable or {@code null} if id is null or not found
   */
  @Override
  public D acquire (Class<D> durableClass, I id) {

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().find(durableClass, id));
  }

  /**
   * Persists the durable via JPA and updates cache if configured.
   *
   * @param durableClass entity class
   * @param durable      durable to persist
   * @return managed durable or {@code null} when input is null
   */
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

  /**
   * Deletes a durable via the EntityManager and clears it from cache if present.
   *
   * @param durableClass entity class
   * @param durable      instance to delete
   */
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

  /**
   * Unsupported for JPA; detaching is managed by the persistence context.
   *
   * @param object durable to detach
   * @return never returns; always throws
   * @throws UnsupportedOperationException always
   */
  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("JPA has no explicit detached state");
  }

  /**
   * Counts all entities of the managed type.
   *
   * @return total count
   */
  @Override
  public long size () {

    return countByQuery(new JPAQueryDetails<D>() {

      /**
       * Builds a QueryDSL query that selects all managed entities for counting.
       *
       * @param query base query to complete
       * @return query selecting every managed entity
       */
      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  /**
   * Lists all entities of the managed type.
   *
   * @return list of all durables
   */
  @Override
  public List<D> list () {

    return listByQuery(new JPAQueryDetails<D>() {

      /**
       * Builds a QueryDSL query that selects all managed entities.
       *
       * @param query base query to complete
       * @return query selecting every managed entity
       */
      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  /**
   * Lists a limited number of entities.
   *
   * @param fetchSize maximum results to return
   * @return limited result list
   */
  @Override
  public List<D> list (int fetchSize) {

    return listByQuery(new JPAQueryDetails<D>() {

      /**
       * Builds a QueryDSL query that selects all managed entities with a limit.
       *
       * @param query base query to complete
       * @return query selecting every managed entity with a fetch size cap
       */
      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity")).limit(fetchSize);
      }
    });
  }

  /**
   * Lists durables with id greater than the supplied lower bound.
   *
   * @param greaterThan exclusive lower bound id
   * @param fetchSize   maximum results to return
   * @return list of durables beyond the supplied id
   */
  @Override
  public List<D> list (I greaterThan, int fetchSize) {

    return listByQuery(new JPAQueryDetails<D>() {

      /**
       * Builds a QueryDSL query selecting entities whose ids are greater than the supplied bound.
       *
       * @param query base query to complete
       * @return query filtered and ordered by id with an optional limit
       */
      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        PathBuilder<D> entityPath = new PathBuilder<>(getManagedClass(), "entity");
        Path<D> idPath = Expressions.path(getManagedClass(), entityPath, "id");

        return query.from(entityPath).where(Expressions.predicate(Ops.GT, idPath, Expressions.constant(greaterThan))).orderBy(new OrderSpecifier<D>(Order.ASC, idPath)).limit(fetchSize);
      }
    });
  }

  /**
   * Lists durables whose ids are contained in the provided collection.
   *
   * @param idCollection identifiers to fetch
   * @return matching durables, or empty list when input is null/empty
   */
  @Override
  public List<D> list (Collection<I> idCollection) {

    if ((idCollection == null) || idCollection.isEmpty()) {

      return Collections.emptyList();
    } else {

      return listByQuery(new JPAQueryDetails<D>() {

        /**
         * Builds a QueryDSL query selecting entities whose ids are contained in the provided collection.
         *
         * @param query base query to complete
         * @return query filtered by the id collection
         */
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

  /**
   * Streams all entities using a QueryDSL query with lazy iteration.
   *
   * @return iterable over all durables
   */
  @Override
  public Iterable<D> scroll () {

    return scrollByQuery(new JPAQueryDetails<D>() {

      /**
       * Builds a QueryDSL query that selects all managed entities for scrolling.
       *
       * @param query base query to complete
       * @return query selecting every managed entity
       */
      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity"));
      }
    });
  }

  /**
   * Streams entities in batches of the given size.
   *
   * @param fetchSize batch size hint
   * @return iterable over durables
   */
  @Override
  public Iterable<D> scroll (int fetchSize) {

    return scrollByQuery(new JPAQueryDetails<D>() {

      /**
       * Builds a QueryDSL query selecting all managed entities with a limit for scrolling.
       *
       * @param query base query to complete
       * @return query selecting every managed entity with a fetch size cap
       */
      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        return query.from(new EntityPathBase<>(getManagedClass(), "entity")).limit(fetchSize);
      }
    });
  }

  /**
   * Streams entities whose ids are greater than the supplied value.
   *
   * @param greaterThan lower bound id (exclusive)
   * @param fetchSize   batch size hint
   * @return iterable over durables beyond the supplied id
   */
  @Override
  public Iterable<D> scrollById (I greaterThan, int fetchSize) {

    return scrollByQuery(new JPAQueryDetails<D>() {

      /**
       * Builds a QueryDSL query selecting entities whose ids exceed the provided lower bound.
       *
       * @param query base query to complete
       * @return query filtered and ordered by id with an optional limit
       */
      @Override
      public JPAQuery<D> completeQuery (JPAQuery<D> query) {

        PathBuilder<D> entityPath = new PathBuilder<>(getManagedClass(), "entity");
        Path<D> idPath = Expressions.path(getManagedClass(), entityPath, "id");

        return query.from(entityPath).where(Expressions.predicate(Ops.GT, idPath, Expressions.constant(greaterThan))).orderBy(new OrderSpecifier<D>(Order.ASC, idPath)).limit(fetchSize);
      }
    });
  }

  /**
   * Counts results for the provided QueryDSL query details.
   *
   * @param queryDetails query specification
   * @param <T>          result type
   * @return count of matching rows
   */
  public <T> long countByQuery (JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? 0 : constructedQuery.fetchCount();
  }

  /**
   * Executes a QueryDSL query and returns a single managed durable.
   *
   * @param queryDetails query specification
   * @return fetched durable or {@code null} if none
   */
  public D findByQuery (JPAQueryDetails<D> queryDetails) {

    JPAQuery<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : getManagedClass().cast(constructedQuery.fetchOne());
  }

  /**
   * Executes a QueryDSL query and returns a single result cast to the provided type.
   *
   * @param returnType   expected return type
   * @param queryDetails query specification
   * @param <T>          result type
   * @return fetched value or {@code null} if none
   */
  public <T> T findByQuery (Class<T> returnType, JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : returnType.cast(constructedQuery.fetchOne());
  }

  /**
   * Executes a QueryDSL query and returns results as managed type durables.
   *
   * @param queryDetails query specification
   * @return checked list of results
   */
  public List<D> listByQuery (JPAQueryDetails<D> queryDetails) {

    JPAQuery<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? Collections.emptyList() : Collections.checkedList(constructedQuery.fetch(), getManagedClass());
  }

  /**
   * Executes a QueryDSL query and returns results as the requested type.
   *
   * @param returnType   expected element type
   * @param queryDetails query specification
   * @param <T>          element type
   * @return checked list of results
   */
  public <T> List<T> listByQuery (Class<T> returnType, JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? Collections.emptyList() : Collections.checkedList(constructedQuery.fetch(), returnType);
  }

  /**
   * Streams results of a QueryDSL query using iterator semantics.
   *
   * @param queryDetails query specification
   * @param <T>          element type
   * @return iterable over the query results
   */
  public <T> Iterable<T> scrollByQuery (JPAQueryDetails<T> queryDetails) {

    JPAQuery<T> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? new EmptyIterable<>() : new IterableIterator<>(constructedQuery.iterate());
  }

  /**
   * Executes a QueryDSL update statement.
   *
   * @param updateDetails update specification
   * @return number of rows affected
   */
  public Long updateWithQuery (JPAUpdateDetails<D> updateDetails) {

    JPAUpdateClause updateClause;

    return ((updateClause = constructUpdate(updateDetails)) == null) ? 0 : updateClause.execute();
  }

  /**
   * Executes a QueryDSL delete statement.
   *
   * @param deleteDetails delete specification
   * @return number of rows deleted
   */
  public Long deleteWithQuery (JPADeleteDetails<D> deleteDetails) {

    JPADeleteClause deleteClause;

    return ((deleteClause = constructDelete(deleteDetails)) == null) ? 0 : deleteClause.execute();
  }

  /**
   * Builds a QueryDSL query from the provided details, applying any fetch graphs.
   *
   * @param queryDetails query specification
   * @param <T>          result type
   * @return constructed query ready for execution
   */
  private <T> JPAQuery<T> constructQuery (JPAQueryDetails<T> queryDetails) {

    EntityManager entityManager = getSession().getNativeSession();
    JPAQuery<T> query = new JPAQuery<>(entityManager);
    String graph;

    if ((graph = queryDetails.getGraph()) != null) {
      query.setHint("jakarta.persistence.fetchgraph", entityManager.getEntityGraph(graph));
    }

    return queryDetails.completeQuery(query);
  }

  /**
   * Builds a QueryDSL update clause using the managed EntityManager.
   *
   * @param updateDetails update specification
   * @param <T>           entity type
   * @return populated update clause
   */
  private <T> JPAUpdateClause constructUpdate (JPAUpdateDetails<T> updateDetails) {

    JPAUpdateClause updateClause = new JPAUpdateClause(getSession().getNativeSession(), updateDetails.getEntityPath());

    return updateDetails.completeUpdate(updateClause);
  }

  /**
   * Builds a QueryDSL delete clause using the managed EntityManager.
   *
   * @param deleteDetails delete specification
   * @param <T>           entity type
   * @return populated delete clause
   */
  private <T> JPADeleteClause constructDelete (JPADeleteDetails<T> deleteDetails) {

    JPADeleteClause deleteClause = new JPADeleteClause(getSession().getNativeSession(), deleteDetails.getEntityPath());

    return deleteDetails.completeDelete(deleteClause);
  }
}
