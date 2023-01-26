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
package org.smallmind.persistence.orm.data.mongo;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.smallmind.nutsnbolts.util.EmptyIterable;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MongoDataDao<I extends Serializable & Comparable<I>, D extends MongoDataDurable<I, D>> extends ORMDao<I, D, MongoTemplateFactory, MongoTemplate> {

  public MongoDataDao (MongoDataProxySession proxySession) {

    this(proxySession, null);
  }

  public MongoDataDao (MongoDataProxySession proxySession, VectoredDao<I, D> vectoredDao) {

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

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().findOne(Query.query(Criteria.where("_id").is(id)), durableClass));
  }

  @Override
  public List<D> list () {

    return getSession().getNativeSession().findAll(getManagedClass());
  }

  @Override
  public List<D> list (int maxResults) {

    return getSession().getNativeSession().find(new Query().limit(maxResults), getManagedClass());
  }

  @Override
  public List<D> list (I greaterThan, int maxResults) {

    return getSession().getNativeSession().find(Query.query(Criteria.where("_id").gt(greaterThan)).with(Sort.by("_id").ascending()).limit(maxResults), getManagedClass());
  }

  @Override
  public List<D> list (Collection<I> idCollection) {

    return getSession().getNativeSession().find(Query.query(Criteria.where("_id").in(idCollection)), getManagedClass());
  }

  @Override
  public Iterable<D> scroll () {

    return new IterableIterator<>(getSession().getNativeSession().stream(new Query(), getManagedClass()));
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return new IterableIterator<>(getSession().getNativeSession().stream(new Query().cursorBatchSize(fetchSize), getManagedClass()));
  }

  @Override
  public Iterable<D> scrollById (final I greaterThan, final int fetchSize) {

    return new IterableIterator<>(getSession().getNativeSession().stream(Query.query(Criteria.where("_id").gt(greaterThan)).with(Sort.by("_id").ascending()).cursorBatchSize(fetchSize), getManagedClass()));
  }

  @Override
  public long size () {

    return getSession().getNativeSession().count(new Query(), getManagedClass());
  }

  @Override
  public D persist (Class<D> durableClass, D durable) {

    if (durable != null) {

      VectoredDao<I, D> vectoredDao = getVectoredDao();

      getSession().getNativeSession().save(durable);

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

      getSession().getNativeSession().remove(durable);

      if (vectoredDao != null) {
        vectoredDao.delete(durableClass, durable);
      }
    }
  }

  @Override
  public D detach (D object) {

    throw new UnsupportedOperationException("Morphia has no explicit detached state");
  }

  public long countByQuery (CountQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? 0 : getSession().getNativeSession().count(constructedQuery, getManagedClass());
  }

  public D findByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? null : getSession().getNativeSession().findOne(constructedQuery, getManagedClass());
  }

  public List<D> listByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    if ((constructedQuery = constructQuery(queryDetails)) == null) {

      return Collections.emptyList();
    } else {

      return getSession().getNativeSession().find(constructedQuery, getManagedClass());
    }
  }

  public Iterable<D> scrollByQuery (FindQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? new EmptyIterable<>() : new IterableIterator<>(getSession().getNativeSession().stream(constructedQuery, getManagedClass()));
  }

  public DeleteResult deleteByQuery (DeleteQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? DeleteResult.unacknowledged() : getSession().getNativeSession().remove(constructedQuery, getManagedClass());
  }

  public UpdateResult updateByQuery (UpdateQueryDetails queryDetails) {

    Query constructedQuery;

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? UpdateResult.unacknowledged() : getSession().getNativeSession().upsert(constructedQuery, queryDetails.completeUpdates(new Update()), getManagedClass());
  }

  public Query constructQuery (QueryDetails queryDetails) {

    return queryDetails.completeQuery(new Query());
  }
}
