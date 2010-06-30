package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.quorum.cache.Cache;

public interface CacheDomain<I extends Comparable<I>, D extends Durable<I>> {

   public abstract String getStatisticsSource ();

   public abstract Cache<String, D> getInstanceCache (Class<D> managedClass);

   public abstract Cache<String, DurableVector<I, D>> getVectorCache (Class<D> managedClass);
}
