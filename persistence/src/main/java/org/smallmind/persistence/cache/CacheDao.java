package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.quorum.cache.Cache;

public interface CacheDao<I extends Comparable<I>, D extends Durable<I>> extends VectoredDao<I, D> {

   public Cache<String, D> getInstanceCache (Class<D> durableClass);

   public Cache<String, DurableVector<I, D>> getVectorCache (Class<D> durableClass);
}
