/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import java.util.List;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.InsertOptions;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryImpl;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
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

    return (id == null) ? null : durableClass.cast(getSession().getNativeSession().get(durableClass, id));
  }

  @Override
  public List<D> list () {

    return getSession().getNativeSession().createQuery(getManagedClass()).asList();
  }

  @Override
  public List<D> list (int maxResults) {

    return getSession().getNativeSession().createQuery(getManagedClass()).asList(new FindOptions().limit(maxResults));
  }

  @Override
  public List<D> list (I greaterThan, int maxResults) {

    return getSession().getNativeSession().createQuery(getManagedClass()).field(Mapper.ID_KEY).greaterThan(greaterThan).order(Mapper.ID_KEY).asList(new FindOptions().limit(maxResults));
  }

  @Override
  public List<D> list (Collection<I> idCollection) {

    return getSession().getNativeSession().createQuery(getManagedClass()).field(Mapper.ID_KEY).in(idCollection).asList();
  }

  @Override
  public Iterable<D> scroll () {

    return new AutoCloseMorphiaIterable<>(getSession().getNativeSession().createQuery(getManagedClass()).fetch());
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return new AutoCloseMorphiaIterable<>(getSession().getNativeSession().createQuery(getManagedClass()).fetch(new FindOptions().batchSize(fetchSize)));
  }

  @Override
  public Iterable<D> scrollById (final I greaterThan, final int fetchSize) {

    return new AutoCloseMorphiaIterable<>(getSession().getNativeSession().createQuery(getManagedClass()).field(Mapper.ID_KEY).greaterThan(greaterThan).order(Mapper.ID_KEY).fetch(new FindOptions().batchSize(fetchSize)));
  }

  @Override
  public long size () {

    return getSession().getNativeSession().createQuery(getManagedClass()).count();
  }

  @Override
  public D persist (Class<D> durableClass, D durable) {

    return persist(durableClass, durable, new InsertOptions().writeConcern(WriteConcern.JOURNALED));
  }

  public D persist (Class<D> durableClass, D durable, InsertOptions insertOptions) {

    if (durable != null) {

      VectoredDao<I, D> vectoredDao = getVectoredDao();

      getSession().getNativeSession().save(durable, insertOptions);

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

      getSession().getNativeSession().delete(durableClass, durable.getId());

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

    return constructQuery(queryDetails).count(queryDetails.getCountOptions());
  }

  public D findByQuery (FindQueryDetails<D> queryDetails) {

    return constructQuery(queryDetails).get(queryDetails.getFindOptions());
  }

  public List<D> listByQuery (FindQueryDetails<D> queryDetails) {

    return constructQuery(queryDetails).asList(queryDetails.getFindOptions());
  }

  public Iterable<D> scrollByQuery (FindQueryDetails<D> queryDetails) {

    return new AutoCloseMorphiaIterable<>(constructQuery(queryDetails).fetch(queryDetails.getFindOptions()));
  }

  public WriteResult deleteByQuery (DeleteQueryDetails<D> queryDetails) {

    return getSession().getNativeSession().delete(constructQuery(queryDetails), queryDetails.getDeleteOptions());
  }

  public UpdateResults updateByQuery (UpdateQueryDetails<D> updateQueryDetails) {

    Query<D> query = getSession().getNativeSession().createQuery(getManagedClass());
    UpdateOperations<D> update = getSession().getNativeSession().createUpdateOperations(getManagedClass());

    return getSession().getNativeSession().update(updateQueryDetails.completeQuery(query), updateQueryDetails.completeUpdates(update), updateQueryDetails.getUpdateOptions());
  }

  public Query<D> constructQuery (QueryDetails<D> queryDetails) {

    Query<D> query = getSession().getNativeSession().createQuery(getManagedClass());

    return queryDetails.completeQuery(query);
  }

  public Query<D> constructRawQuery (String rawJson) {

    Query<D> query = getSession().getNativeSession().createQuery(getManagedClass());

    ((QueryImpl)query).setQueryObject((DBObject)JSON.parse(rawJson));

    return query;
  }
}
