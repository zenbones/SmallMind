package org.smallmind.persistence.orm.spring.data.mongo;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.mongodb.WriteConcern;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.smallmind.nutsnbolts.util.EmptyIterable;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.morphia.AutoCloseMorphiaIterable;
import org.smallmind.persistence.orm.morphia.CountQueryDetails;
import org.smallmind.persistence.orm.morphia.DeleteQueryDetails;
import org.smallmind.persistence.orm.morphia.FindQueryDetails;
import org.smallmind.persistence.orm.morphia.MorphiaUpdates;
import org.smallmind.persistence.orm.morphia.QueryDetails;
import org.smallmind.persistence.orm.morphia.UpdateQueryDetails;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

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

    return new IterableIterator<>(getSession().getNativeSession().stream(new Query(), getManagedClass()).iterator());
  }

  @Override
  public Iterable<D> scroll (int fetchSize) {

    return new IterableIterator<>(getSession().getNativeSession().stream(new Query().cursorBatchSize(fetchSize), getManagedClass()).iterator());
  }

  @Override
  public Iterable<D> scrollById (final I greaterThan, final int fetchSize) {

    return new IterableIterator<>(getSession().getNativeSession().stream(Query.query(Criteria.where("_id").gt(greaterThan)).with(Sort.by("_id").ascending()).cursorBatchSize(fetchSize), getManagedClass()).iterator());
  }

  @Override
  public long size () {

    return getSession().getNativeSession().count();
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

    return ((constructedQuery = constructQuery(queryDetails)) == null) ? UpdateResult.unacknowledged() : constructedQuery.update(queryDetails.getUpdateOptions(), queryDetails.completeUpdates(new MorphiaUpdates<>(constructedQuery)).getCollected());
  }

  public Query<D> constructQuery (QueryDetails<D> queryDetails) {

    Query<D> query = getSession().getNativeSession().find(getManagedClass());

    return queryDetails.completeQuery(query);
  }
}
