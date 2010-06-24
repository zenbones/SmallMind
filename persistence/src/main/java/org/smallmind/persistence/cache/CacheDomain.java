package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableVector;
import org.smallmind.quorum.cache.LockingCache;

public interface CacheDomain<I, D extends Durable<I>> {

   public abstract LockingCache<I, D> getInstanceCache (Class<D> managedClass);

   public abstract LockingCache<String, DurableVector<I, D>> getVectorCache (Class<D> managedClass);
}
