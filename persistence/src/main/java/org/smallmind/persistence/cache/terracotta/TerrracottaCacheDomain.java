package org.smallmind.persistence.cache.terracotta;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableVector;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.quorum.cache.LockingCache;
import org.smallmind.quorum.cache.LockingCacheManager;
import org.smallmind.quorum.cache.terracotta.TerracottaCacheManager;
import org.smallmind.quorum.cache.terracotta.TerracottaCacheProvider;

public class TerrracottaCacheDomain<I, D extends Durable<I>> implements CacheDomain<I, D> {

   private LockingCacheManager<I, D> instanceCacheManager;
   private LockingCacheManager<String, DurableVector<I, D>> vectorCacheManager;

   public TerrracottaCacheDomain (TerracottaCacheProvider cacheProvider) {

      instanceCacheManager = new TerracottaCacheManager<I, D>(cacheProvider, "instance");
      vectorCacheManager = new TerracottaCacheManager<String, DurableVector<I, D>>(cacheProvider, "value");
   }

   public LockingCache<I, D> getInstanceCache (Class<D> managedClass) {

      return instanceCacheManager.getLockingCache(managedClass.getName());
   }

   public LockingCache<String, DurableVector<I, D>> getVectorCache (Class<D> managedClass) {

      return vectorCacheManager.getLockingCache(managedClass.getName());
   }
}