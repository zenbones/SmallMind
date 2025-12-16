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
package org.smallmind.persistence.orm.jpa;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;

/**
 * ORM DAO implementation for JPA-backed durables with optional vector cache integration.
 *
 * @param <I> identifier type
 * @param <D> durable entity type
 */
public abstract class JPADao<I extends Serializable & Comparable<I>, D extends Durable<I>> extends ORMDao<I, D, EntityManagerFactory, EntityManager> {

  /**
   * Constructs a JPA DAO without cache integration.
   *
   * @param proxySession the JPA proxy session
   */
  public JPADao (JPAProxySession proxySession) {

    this(proxySession, null);
  }

  /**
   * Constructs a JPA DAO with optional cache integration.
   *
   * @param proxySession the JPA proxy session
   * @param vectoredDao  cache-backed delegate used when caching is enabled
   */
  public JPADao (JPAProxySession proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession, vectoredDao);
  }

  /**
   * Retrieves a durable by id, consulting cache first when available.
   *
   * @param durableClass the entity class
   * @param id           the identifier to find
   * @return the durable instance or {@code null} if not found
   */
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
   * Fetches a durable directly from the JPA EntityManager by id.
   *
   * @param durableClass entity class to retrieve
   * @param id           identifier to locate
   * @return managed durable or {@code null} when id is null or not found
   */
  @Override
  public D acquire (Class<D> durableClass, I id) {

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().find(durableClass, id));
  }

  /**
   * Persists a durable via JPA and updates cache if present.
   *
   * @param durableClass the entity class
   * @param durable      the durable to persist
   * @return the managed/persisted durable instance, or {@code null} when input is null
   */
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
   * Removes a durable from persistence and clears it from cache if configured.
   *
   * @param durableClass entity class of the durable
   * @param durable      instance to delete
   */
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

  /**
   * Lists all durables for the managed class.
   *
   * @return list of all durables
   */
  public List<D> list () {

    return constructCriteriaQuery(getManagedClass(), new CriteriaQueryDetails<D>() {

      /**
       * Builds a simple selection criteria for all managed durables.
       *
       * @param criteriaClass   expected durable class
       * @param criteriaBuilder builder used to create the query
       * @return criteria selecting every row for the managed class
       */
      @Override
      public CriteriaQuery<D> completeCriteria (Class<D> criteriaClass, CriteriaBuilder criteriaBuilder) {

        CriteriaQuery<D> query = criteriaBuilder.createQuery(getManagedClass());
        Root<D> root = query.from(getManagedClass());

        return query.select(root);
      }
    }).getResultList();
  }

  /**
   * Lists a limited number of durables.
   *
   * @param fetchSize maximum results to return
   * @return list of durables up to {@code fetchSize}
   */
  public List<D> list (final int fetchSize) {

    return constructCriteriaQuery(getManagedClass(), new CriteriaQueryDetails<D>() {

      /**
       * Builds a simple selection criteria for all managed durables.
       *
       * @param criteriaClass   expected durable class
       * @param criteriaBuilder builder used to create the query
       * @return criteria selecting every row for the managed class
       */
      @Override
      public CriteriaQuery<D> completeCriteria (Class<D> criteriaClass, CriteriaBuilder criteriaBuilder) {

        CriteriaQuery<D> query = criteriaBuilder.createQuery(getManagedClass());
        Root<D> root = query.from(getManagedClass());

        return query.select(root);
      }
    }).setMaxResults(fetchSize).getResultList();
  }

  /**
   * Lists durables whose id is greater than the supplied lower bound.
   *
   * @param greaterThan exclusive lower bound id
   * @param fetchSize   maximum number of results
   * @return list of durables starting after {@code greaterThan}
   */
  public List<D> list (final I greaterThan, final int fetchSize) {

    return constructCriteriaQuery(getManagedClass(), new CriteriaQueryDetails<D>() {

      /**
       * Builds criteria selecting managed durables with ids greater than the supplied bound.
       *
       * @param criteriaClass   expected durable class
       * @param criteriaBuilder builder used to create the query
       * @return criteria filtering by id greater than {@code greaterThan}
       */
      @Override
      public CriteriaQuery<D> completeCriteria (Class<D> criteriaClass, CriteriaBuilder criteriaBuilder) {

        CriteriaQuery<D> query = criteriaBuilder.createQuery(getManagedClass());
        Root<D> root = query.from(getManagedClass());

        return query.select(root).where(criteriaBuilder.gt(root.get("id"), (Number)greaterThan));
      }
    }).setMaxResults(fetchSize).getResultList();
  }

  /**
   * Lists durables whose ids are contained in the provided collection.
   *
   * @param idCollection collection of identifiers to fetch
   * @return matching durables, empty when input is null or empty
   */
  @Override
  public List<D> list (Collection<I> idCollection) {

    if ((idCollection == null) || idCollection.isEmpty()) {

      return Collections.emptyList();
    } else {

      return constructCriteriaQuery(getManagedClass(), new CriteriaQueryDetails<D>() {

        /**
         * Builds criteria selecting managed durables whose ids are contained in the supplied collection.
         *
         * @param criteriaClass   expected durable class
         * @param criteriaBuilder builder used to create the query
         * @return criteria filtering by the provided id set
         */
        @Override
        public CriteriaQuery<D> completeCriteria (Class<D> criteriaClass, CriteriaBuilder criteriaBuilder) {

          CriteriaQuery<D> query = criteriaBuilder.createQuery(getManagedClass());
          Root<D> root = query.from(getManagedClass());

          return query.select(root).where(root.get("id").in(idCollection));
        }
      }).getResultList();
    }
  }

  /**
   * Unsupported in this DAO; basic scroll is not implemented.
   *
   * @return nothing, always throws
   * @throws UnsupportedOperationException always
   */
  public Iterable<D> scroll () {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported in this DAO; JPA scroll is not implemented.
   *
   * @param fetchSize hint for batch size
   * @return nothing, always throws
   * @throws UnsupportedOperationException always
   */
  public Iterable<D> scroll (int fetchSize) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported in this DAO; JPA scroll by id is not implemented.
   *
   * @param greaterThan lower bound id (exclusive)
   * @param fetchSize   hint for batch size
   * @return nothing, always throws
   * @throws UnsupportedOperationException always
   */
  public Iterable<D> scrollById (I greaterThan, int fetchSize) {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported in this DAO; size calculation is not implemented.
   *
   * @return nothing, always throws
   * @throws UnsupportedOperationException always
   */
  public long size () {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported in this DAO; JPA detaches by removing from persistence context rather than explicit state.
   *
   * @param object durable to detach
   * @return never returns, always throws
   * @throws UnsupportedOperationException always
   */
  public D detach (D object) {

    throw new UnsupportedOperationException("JPA has no explicit detached state");
  }

  /**
   * Deletes managed type entities using the supplied criteria.
   *
   * @param criteriaDeleteDetails delete clause details
   * @return number of rows deleted
   */
  public int deleteWithCriteria (CriteriaDeleteDetails<D> criteriaDeleteDetails) {

    return constructCriteriaDelete(getManagedClass(), criteriaDeleteDetails).executeUpdate();
  }

  /**
   * Executes a criteria delete for the specified type.
   *
   * @param criteriaType          entity type to delete from
   * @param criteriaDeleteDetails details of the delete criteria
   * @param <T>                   entity type
   * @return number of rows deleted
   */
  public <T> int deleteWithCriteria (Class<T> criteriaType, CriteriaDeleteDetails<T> criteriaDeleteDetails) {

    return constructCriteriaDelete(criteriaType, criteriaDeleteDetails).executeUpdate();
  }

  /**
   * Executes an arbitrary JPQL update/delete query.
   *
   * @param queryDetails describes the query to execute
   * @return number of rows affected
   */
  public int executeWithQuery (QueryDetails queryDetails) {

    return constructQuery(queryDetails).executeUpdate();
  }

  /**
   * Executes a criteria update for the managed type.
   *
   * @param criteriaUpdateDetails update details
   * @return number of rows updated
   */
  public int executeWithCriteria (CriteriaUpdateDetails<D> criteriaUpdateDetails) {

    return constructCriteriaUpdate(getManagedClass(), criteriaUpdateDetails).executeUpdate();
  }

  /**
   * Executes a criteria update for the provided type.
   *
   * @param criteriaType          entity type to update
   * @param criteriaUpdateDetails update details
   * @param <T>                   entity type
   * @return number of rows updated
   */
  public <T> int executeWithCriteria (Class<T> criteriaType, CriteriaUpdateDetails<T> criteriaUpdateDetails) {

    return constructCriteriaUpdate(criteriaType, criteriaUpdateDetails).executeUpdate();
  }

  /**
   * Executes a query and returns a single result cast to the requested type.
   *
   * @param returnType   expected return type
   * @param queryDetails query to execute
   * @param <T>          result type
   * @return the single result
   */
  public <T> T findByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return returnType.cast(constructQuery(queryDetails).getSingleResult());
  }

  /**
   * Executes a query and returns a single durable of the managed type.
   *
   * @param queryDetails query to execute
   * @return the single result
   */
  public D findByQuery (QueryDetails queryDetails) {

    return getManagedClass().cast(constructQuery(queryDetails).getSingleResult());
  }

  /**
   * Executes a criteria query and returns a single result cast to the requested type.
   *
   * @param returnType           expected return type
   * @param criteriaQueryDetails criteria query to execute
   * @param <T>                  result type
   * @return the single result
   */
  public <T> T findByCriteria (Class<T> returnType, CriteriaQueryDetails<T> criteriaQueryDetails) {

    return returnType.cast(constructCriteriaQuery(returnType, criteriaQueryDetails).getSingleResult());
  }

  /**
   * Executes a criteria query and returns a single durable of the managed type.
   *
   * @param criteriaQueryDetails criteria query to execute
   * @return the single result
   */
  public D findByCriteria (CriteriaQueryDetails<D> criteriaQueryDetails) {

    return getManagedClass().cast(constructCriteriaQuery(getManagedClass(), criteriaQueryDetails).getSingleResult());
  }

  /**
   * Executes a query and returns a checked list of the requested type.
   *
   * @param returnType   expected element type
   * @param queryDetails query to execute
   * @param <T>          element type
   * @return results as a checked list
   */
  public <T> List<T> listByQuery (Class<T> returnType, QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).getResultList(), returnType);
  }

  /**
   * Executes a query and returns results as managed type durables.
   *
   * @param queryDetails query to execute
   * @return results as a checked list
   */
  public List<D> listByQuery (QueryDetails queryDetails) {

    return Collections.checkedList(constructQuery(queryDetails).getResultList(), getManagedClass());
  }

  /**
   * Executes a criteria query and returns a checked list of the requested type.
   *
   * @param returnType           expected element type
   * @param criteriaQueryDetails criteria query to execute
   * @param <T>                  element type
   * @return results as a checked list
   */
  public <T> List<T> listByCriteria (Class<T> returnType, CriteriaQueryDetails<T> criteriaQueryDetails) {

    return Collections.checkedList(constructCriteriaQuery(returnType, criteriaQueryDetails).getResultList(), returnType);
  }

  /**
   * Executes a criteria query and returns results as managed type durables.
   *
   * @param criteriaQueryDetails criteria query to execute
   * @return results as a checked list
   */
  public List<D> listByCriteria (CriteriaQueryDetails<D> criteriaQueryDetails) {

    return Collections.checkedList(constructCriteriaQuery(getManagedClass(), criteriaQueryDetails).getResultList(), getManagedClass());
  }

  /**
   * Builds a JPQL query from the provided details.
   *
   * @param queryDetails query specification
   * @return constructed query ready for execution
   */
  public Query constructQuery (QueryDetails queryDetails) {

    return queryDetails.completeQuery(getSession().getNativeSession().createQuery(queryDetails.getQueryString()));
  }

  /**
   * Builds a criteria query of the given result type.
   *
   * @param criteriaClass        result type
   * @param criteriaQueryDetails query specification
   * @param <T>                  result type
   * @return constructed typed query
   */
  public <T> TypedQuery<T> constructCriteriaQuery (Class<T> criteriaClass, CriteriaQueryDetails<T> criteriaQueryDetails) {

    EntityManager entityManager = getSession().getNativeSession();

    return entityManager.createQuery(criteriaQueryDetails.completeCriteria(criteriaClass, entityManager.getCriteriaBuilder()));
  }

  /**
   * Builds a criteria update query for the given entity type.
   *
   * @param criteriaClass         entity type
   * @param criteriaUpdateDetails update specification
   * @param <T>                   entity type
   * @return constructed query ready for execution
   */
  public <T> Query constructCriteriaUpdate (Class<T> criteriaClass, CriteriaUpdateDetails<T> criteriaUpdateDetails) {

    EntityManager entityManager = getSession().getNativeSession();

    return entityManager.createQuery(criteriaUpdateDetails.completeCriteria(criteriaClass, entityManager.getCriteriaBuilder()));
  }

  /**
   * Builds a criteria delete query for the given entity type.
   *
   * @param criteriaClass         entity type
   * @param criteriaDeleteDetails delete specification
   * @param <T>                   entity type
   * @return constructed query ready for execution
   */
  public <T> Query constructCriteriaDelete (Class<T> criteriaClass, CriteriaDeleteDetails<T> criteriaDeleteDetails) {

    EntityManager entityManager = getSession().getNativeSession();

    return entityManager.createQuery(criteriaDeleteDetails.completeCriteria(criteriaClass, entityManager.getCriteriaBuilder()));
  }
}
