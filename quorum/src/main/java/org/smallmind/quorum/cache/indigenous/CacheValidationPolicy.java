package org.smallmind.quorum.cache.indigenous;

public interface CacheValidationPolicy<E extends CacheEntry> {

   public abstract boolean isValid (E cacheEntry);
}
