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
package org.smallmind.persistence.orm.morphia;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.mongodb.WriteConcern;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.InsertOneOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filters;
import org.smallmind.nutsnbolts.util.EmptyIterable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;

public class MorphiaDao<I extends Serializable & Comparable<I>, D extends MorphiaDurable<I, D>> extends ORMDao<I, D, DataStoreFactory, Datastore> {

  public MorphiaDao (MorphiaProxySession proxySession) {

    this(proxySession, null);
  }

  public MorphiaDao (MorphiaProxySession proxySession, VectoredDao<I, D> vectoredDao) {

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

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().find(durableClass).filter(Filters.eq("_id", id)).first());
  }

  @Override
  public List<D> list () {

    try (MorphiaCursor<D> cursor = getSession().getNativeSession().find(getManagedClass()).iterator()) {

      return cursor.toList();
    }
  }

  @Override
  public List<D> list (int maxResults) {

    try (MorphiaCursor<D> cursor = getSession().getNativeSession().find(getManagedClass()).iterator(new FindOptions().limit(maxResults))) {

      return cursor.toList();
    }
  }

  @Override
  public List<D> list (I greaterThan, int maxResults) {

    try (MorphiaCursor<D> cursor = getSession().getNativeSession().find(getManagedClass()).filter(Filters.gt("_id", greaterThan)).iterator(new FindOptions().sort(Sort.ascending("_id")).limit(maxResults))) {

      return cursor.toList();
    }
  }

  @Override
  public List<D> list (Collection<I> idCollection) {

    try (MorphiaCursor<D> cursor = getSession().getNativeSession().find(getManagedClass()).filter(Filters.in("_id", idCollection)).iterator()) {

      return cursor.toList();
    }
  }

  @Override
  public Iterable<D> scroll () {

    return new AutoCloseMorphiaIterable<>(getSession().getNativeSession().find(getManagedClass()).iterator());
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return new AutoCloseMorphiaIterable<>(getSession().getNativeSession().find(getManagedClass()).iterator(new FindOptions().batchSize(fetchSize)));
  }

  @Override
  public Iterable<D> scrollById (final I greaterThan, final int fetchSize) {

    return new AutoCloseMorphiaIterable<>(getSession().getNativeSession().find(getManagedClass()).filter(Filters.gt("_id", greaterThan)).iterator(new FindOptions().sort(Sort.ascending("_id")).batchSize(fetchSize)));
  }

  @Override
  public long size () {

    return getSession().getNativeSession().find(getManagedClass()).count();
  }

  @Override
  public D persist (Class<D> durableClass, D durable) {

    return persist(durableClass, durable, new InsertOneOptions().writeConcern(WriteConcern.JOURNALED));
  }

  public D persist (D durable, InsertOneOptions insertOneOptions) {

    return persist(getManagedClass(), durable, insertOneOptions);
  }

  public D persist (Class<D> durableClass, D durable, InsertOneOptions insertOneOptions) {

    if (durable != null) {

      VectoredDao<I, D> vectoredDao = getVectoredDao();

      getSession().getNativeSession().save(durable, insertOneOptions);

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

      getSession().getNativeSession().find(durableClass).filter(Filters.eq("_id", durable.getId())).delete();

      if (vectoredDao != null) {
        vectoredDao.delete(durableClass, durable);
      }
    }
  }

  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("Morphia has no explicit detached state");
  }

  public long countByQuery (CountQueryDetails<D> queryDetails) {

    Query<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? 0 : constructedQuery.count(queryDetails.getCountOptions());
  }

  public D findByQuery (FindQueryDetails<D> queryDetails) {

    Query<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : constructedQuery.first(queryDetails.getFindOptions());
  }

  public List<D> listByQuery (FindQueryDetails<D> queryDetails) {

    Query<D> constructedQuery;

    if ((constructedQuery = constructQuery(queryDetails)) == null) {

      return Collections.emptyList();
    } else {
      try (MorphiaCursor<D> cursor = constructedQuery.iterator(queryDetails.getFindOptions())) {

        return cursor.toList();
      }
    }
  }

  public Iterable<D> scrollByQuery (FindQueryDetails<D> queryDetails) {

    Query<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? new EmptyIterable<>() : new AutoCloseMorphiaIterable<>(constructedQuery.iterator(queryDetails.getFindOptions()));
  }

  public DeleteResult deleteByQuery (DeleteQueryDetails<D> queryDetails) {

    Query<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? DeleteResult.unacknowledged() : constructedQuery.delete(queryDetails.getDeleteOptions());
  }

  public UpdateResult updateByQuery (UpdateQueryDetails<D> queryDetails) {

    Query<D> constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? UpdateResult.unacknowledged() : constructedQuery.update(queryDetails.getUpdateOptions(), queryDetails.completeUpdates());
  }

  public Query<D> constructQuery (QueryDetails<D> queryDetails) {

    Query<D> query = getSession().getNativeSession().find(getManagedClass());

    return queryDetails.completeQuery(query);
  }
}
