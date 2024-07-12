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

public class ThrongDao<I extends Serializable & Comparable<I>, D extends ThrongDurable<I, D>> extends ORMDao<I, D, ThrongClientFactory, ThrongClient> {

  public ThrongDao (ThrongProxySession proxySession) {

    this(proxySession, null);
  }

  public ThrongDao (ThrongProxySession proxySession, VectoredDao<I, D> vectoredDao) {

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

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().findOne(durableClass, Query.with().filter(Filter.where("_id").eq("id"))));
  }

  @Override
  public List<D> list () {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty()).asList();
  }

  @Override
  public List<D> list (int maxResults) {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty().limit(maxResults)).asList();
  }

  @Override
  public List<D> list (I greaterThan, int maxResults) {

    return getSession().getNativeSession().find(getManagedClass(), Query.with().filter(Filter.where("_id").gt(greaterThan)).sort(Sort.on().asc("_id")).limit(maxResults)).asList();
  }

  @Override
  public List<D> list (Collection<I> idCollection) {

    return getSession().getNativeSession().find(getManagedClass(), Query.with().filter(Filter.where("_id").in(idCollection))).asList();
  }

  @Override
  public Iterable<D> scroll () {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty());
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return getSession().getNativeSession().find(getManagedClass(), Query.empty().batchSize(fetchSize));
  }

  @Override
  public Iterable<D> scrollById (final I greaterThan, final int fetchSize) {

    return getSession().getNativeSession().find(getManagedClass(), Query.with().filter(Filter.where("_id").gt(greaterThan)).sort(Sort.on().asc("_id")).batchSize(fetchSize));
  }

  @Override
  public long size () {

    return getSession().getNativeSession().count(getManagedClass(), Filter.empty());
  }

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

  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("Throng has no explicit detached state");
  }

  public long countByFilter (CountFilterDetails filterDetails) {

    Filters constructedFilters;

    return ((constructedFilters = constructFilters(filterDetails)) == null) ? 0 : getSession().getNativeSession().count(getManagedClass(), constructedFilters.combine());
  }

  public D findByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : getSession().getNativeSession().findOne(getManagedClass(), constructedQuery);
  }

  public List<D> listByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    if ((constructedQuery = constructQuery(queryDetails)) == null) {

      return Collections.emptyList();
    } else {

      return getSession().getNativeSession().find(getManagedClass(), constructedQuery).asList();
    }
  }

  public Iterable<D> scrollByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? new EmptyIterable<>() : getSession().getNativeSession().find(getManagedClass(), constructedQuery);
  }

  public DeleteResult deleteByFilter (DeleteFilterDetails filterDetails) {

    Filters constructedFilters;

    return ((constructedFilters = constructFilters(filterDetails)) == null) ? DeleteResult.unacknowledged() : getSession().getNativeSession().delete(getManagedClass(), constructedFilters.combine(), filterDetails.getDeleteOptions());
  }

  public UpdateResult updateByFilter (UpdateFilterDetails filterDetails) {

    Filters constructedFilters;

    return ((constructedFilters = constructFilters(filterDetails)) == null) ? UpdateResult.unacknowledged() : getSession().getNativeSession().update(getManagedClass(), constructedFilters.combine(), filterDetails.completeUpdates(Updates.of()), filterDetails.getUpdateOptions());
  }

  public Query constructQuery (QueryDetails queryDetails) {

    return queryDetails.completeQuery(Query.with());
  }

  public Filters constructFilters (FilterDetails filterDetails) {

    return filterDetails.completeFilter(Filters.on());
  }
}
