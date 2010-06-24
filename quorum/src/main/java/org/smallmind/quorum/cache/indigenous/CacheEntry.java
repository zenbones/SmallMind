package org.smallmind.quorum.cache.indigenous;

public interface CacheEntry<V> {

   public abstract V getEntry ();

   public abstract void cacheHit ();

   public abstract void expire ();

   public abstract void close ();
}
