package org.smallmind.quorum.cache.indigenous;

import java.util.concurrent.atomic.AtomicInteger;

public class SizeLimitedCacheAccumulator<D extends CacheMetaData, K, E extends OrderedCacheEntry<D, ?>> implements CacheAccumulator<D, K, E> {

   private AtomicInteger size = new AtomicInteger(0);
   private int limit;

   public SizeLimitedCacheAccumulator (int limit) {

      this.limit = limit;
   }

   public void add (D cacheMetaData) {

      size.getAndIncrement();
   }

   public void remove (D cacheMetaData) {

      size.getAndDecrement();
   }

   public boolean isOverLimit () {

      return size.get() > limit;
   }
}
