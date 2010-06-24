package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableVector;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.quorum.cache.LockingCache;

public interface CacheDao<I, D extends Durable<I>> extends VectoredDao<I, D> {

   public LockingCache<I, D> getInstanceCache (Class<D> durableClass);

   public LockingCache<String, DurableVector<I, D>> getVectorCache (Class<D> durableClass);
}