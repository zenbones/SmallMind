package org.smallmind.quorum.cache.indigenous;

public interface CacheExpirationPolicy<E extends CacheEntry> {

   public abstract int getTimerTickSeconds ();

   public abstract boolean isStale (E cacheEntry);
}
