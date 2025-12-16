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
package org.smallmind.persistence.orm.throng;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.result.DeleteResult;
import org.smallmind.mongodb.throng.ThrongClient;
import org.smallmind.mongodb.throng.UpdateResult;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Query;
import org.smallmind.mongodb.throng.query.Sort;
import org.smallmind.mongodb.throng.query.Updates;
import org.smallmind.nutsnbolts.util.EmptyIterable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;

/**
 * DAO implementation for Throng (MongoDB) durables, with optional vector cache integration.
 *
 * @param <I> identifier type
 * @param <D> durable entity type
 */
public class ThrongDao<I extends Serializable & Comparable<I>, D extends ThrongDurable<I, D>> extends ORMDao<I, D, ThrongClientFactory, ThrongClient> {

  /**
   * Constructs a Throng DAO without cache integration.
   *
   * @param proxySession the Throng proxy session
   */
  public ThrongDao (ThrongProxySession proxySession) {

    this(proxySession, null);
  }

  /**
   * Constructs a Throng DAO with optional cache integration.
   *
   * @param proxySession the Throng proxy session
   * @param vectoredDao  cache-backed delegate used when caching is enabled
   */
  public ThrongDao (ThrongProxySession proxySession, VectoredDao<I, D> vectoredDao) {

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
   * Fetches a durable directly from MongoDB by id.
   *
   * @param durableClass the entity class
   * @param id           identifier to find
   * @return the durable instance or {@code null} if not found
   */
  @Override
  public D acquire (Class<D> durableClass, I id) {

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().findOne(durableClass, Query.with().filter(Filter.where("_id").eq("id"))));
  }

  /**
   * Lists all durables.
   *
   * @return list of all entities
   */
  @Override
  public List<D> list () {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty()).asList();
  }

  /**
   * Lists durables with a maximum result count.
   *
   * @param maxResults maximum number of results
   * @return limited list of entities
   */
  @Override
  public List<D> list (int maxResults) {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty().limit(maxResults)).asList();
  }

  /**
   * Lists durables with ids greater than a value, sorted ascending, limited to {@code maxResults}.
   *
   * @param greaterThan lower bound id (exclusive)
   * @param maxResults  maximum number of results
   * @return limited list of entities
   */
  @Override
  public List<D> list (I greaterThan, int maxResults) {

    return getSession().getNativeSession().find(getManagedClass(), Query.with().filter(Filter.where("_id").gt(greaterThan)).sort(Sort.on().asc("_id")).limit(maxResults)).asList();
  }

  /**
   * Lists durables whose ids are in the provided collection.
   *
   * @param idCollection ids to fetch
   * @return matching entities
   */
  @Override
  public List<D> list (Collection<I> idCollection) {

    return getSession().getNativeSession().find(getManagedClass(), Query.with().filter(Filter.where("_id").in(idCollection))).asList();
  }

  /**
   * Streams all durables.
   *
   * @return iterable over all entities
   */
  @Override
  public Iterable<D> scroll () {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty());
  }

  /**
   * Streams durables with a batch size hint.
   *
   * @param fetchSize batch size
   * @return iterable over entities
   */
  @Override
  public Iterable<D> scroll (int fetchSize) {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty().batchSize(fetchSize));
  }

  /**
   * Streams durables with ids greater than the given value, sorted ascending.
   *
   * @param greaterThan lower bound id (exclusive)
   * @param fetchSize   batch size
   * @return iterable over entities
   */
  @Override
  public Iterable<D> scrollById (final I greaterThan, final int fetchSize) {

    return getSession().getNativeSession().find(getManagedClass(), Query.with().filter(Filter.where("_id").gt(greaterThan)).sort(Sort.on().asc("_id")).batchSize(fetchSize));
  }

  /**
   * Counts all durables.
   *
   * @return total count
   */
  @Override
  public long size () {

    return getSession().getNativeSession().count(getManagedClass(), Filter.empty());
  }

  /**
   * Persists a durable and updates cache if present.
   *
   * @param durableClass entity class
   * @param durable      entity to persist
   * @return persisted entity or {@code null} if input is null
   */
  @Override
  public D persist (Class<D> durableClass, D durable) {

    if (durable != null) {

      VectoredDao<I, D> vectoredDao = getVectoredDao();

      getSession().getNativeSession().insert(durable, new InsertOneOptions());

      if (vectoredDao != null) {

        return vectoredDao.persist(durableClass, durable, UpdateMode.HARD);
      }

      return durable;
    }

    return null;
  }

  /**
   * Deletes a durable and evicts from cache if present.
   *
   * @param durableClass entity class
   * @param durable      entity to delete
   */
  @Override
  public void delete (Class<D> durableClass, D durable) {

    if (durable != null) {

      VectoredDao<I, D> vectoredDao = getVectoredDao();

      getSession().getNativeSession().delete(durableClass, Filter.where("_id").eq(durable.getId()), new DeleteOptions());

      if (vectoredDao != null) {
        vectoredDao.delete(durableClass, durable);
      }
    }
  }

  /**
   * Unsupported; Throng does not expose explicit detach semantics.
   *
   * @param object durable to detach
   * @return never returns, always throws
   * @throws UnsupportedOperationException always
   */
  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("Throng has no explicit detached state");
  }

  /**
   * Counts documents matching the provided filter details.
   *
   * @param filterDetails filter construction details
   * @return number of matching durables
   */
  public long countByFilter (CountFilterDetails filterDetails) {

    Filters constructedFilters;

    return ((constructedFilters = constructFilters(filterDetails)) == null) ? 0 : getSession().getNativeSession().count(getManagedClass(), constructedFilters.combine());
  }

  /**
   * Executes a find query and returns the first matching durable.
   *
   * @param queryDetails query specification
   * @return matching durable or {@code null} if none
   */
  public D findByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : getSession().getNativeSession().findOne(getManagedClass(), constructedQuery);
  }

  /**
   * Executes a find query and returns results as a list.
   *
   * @param queryDetails query specification
   * @return list of matching entities
   */
  public List<D> listByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    if ((constructedQuery = constructQuery(queryDetails)) == null) {

      return Collections.emptyList();
    } else {

      return getSession().getNativeSession().find(getManagedClass(), constructedQuery).asList();
    }
  }

  /**
   * Streams results for the provided query details.
   *
   * @param queryDetails query specification
   * @return iterable over matching durables
   */
  public Iterable<D> scrollByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? new EmptyIterable<>() : getSession().getNativeSession().find(getManagedClass(), constructedQuery);
  }

  /**
   * Executes a delete using the provided filter details.
   *
   * @param filterDetails delete filter configuration
   * @return result of the delete operation
   */
  public DeleteResult deleteByFilter (DeleteFilterDetails filterDetails) {

    Filters constructedFilters;

    return ((constructedFilters = constructFilters(filterDetails)) == null) ? DeleteResult.unacknowledged() : getSession().getNativeSession().delete(getManagedClass(), constructedFilters.combine(), filterDetails.getDeleteOptions());
  }

  /**
   * Executes an update using the provided filter and update configuration.
   *
   * @param filterDetails update filter and options
   * @return result of the update operation
   */
  public UpdateResult updateByFilter (UpdateFilterDetails filterDetails) {

    Filters constructedFilters;

    return ((constructedFilters = constructFilters(filterDetails)) == null) ? UpdateResult.unacknowledged() : getSession().getNativeSession().update(getManagedClass(), constructedFilters.combine(), filterDetails.completeUpdates(Updates.of()), filterDetails.getUpdateOptions());
  }

  /**
   * Builds a query from the provided query details.
   *
   * @param queryDetails query specification
   * @return constructed query
   */
  public Query constructQuery (QueryDetails queryDetails) {

    return queryDetails.completeQuery(Query.with());
  }

  /**
   * Builds filters from the provided filter details.
   *
   * @param filterDetails filter specification
   * @return constructed filters
   */
  public Filters constructFilters (FilterDetails filterDetails) {

    return filterDetails.completeFilter(Filters.on());
  }
}
