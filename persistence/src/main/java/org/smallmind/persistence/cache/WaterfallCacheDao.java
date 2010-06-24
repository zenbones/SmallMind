package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableVector;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.WaterfallDao;
import org.smallmind.quorum.cache.LockingCache;

public abstract class WaterfallCacheDao<I, D extends Durable<I>> implements CacheDao<I, D>, WaterfallDao<I, D> {

   private CacheDomain<I, D> cacheDomain;

   private VectoredDao<I, D> nextDao;

   public WaterfallCacheDao (CacheDomain<I, D> cacheDomain) {

      this(null, cacheDomain);
   }

   public WaterfallCacheDao (VectoredDao<I, D> nextDao, CacheDomain<I, D> cacheDomain) {

      this.nextDao = nextDao;
      this.cacheDomain = cacheDomain;
   }

   public VectoredDao<I, D> getNextDao () {

      return nextDao;
   }

   public LockingCache<I, D> getInstanceCache (Class<D> durableClass) {

      return cacheDomain.getInstanceCache(durableClass);
   }

   public LockingCache<String, DurableVector<I, D>> getVectorCache (Class<D> durableClass) {

      return cacheDomain.getVectorCache(durableClass);
   }
}