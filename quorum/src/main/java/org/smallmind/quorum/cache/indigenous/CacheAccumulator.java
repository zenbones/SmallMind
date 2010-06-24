package org.smallmind.quorum.cache.indigenous;

public interface CacheAccumulator<D extends CacheMetaData, K, E extends OrderedCacheEntry<D, ?>> {

   public abstract void add (D cacheMetaData);

   public abstract void remove (D cacheMetaData);

   public abstract boolean isOverLimit ();
}
