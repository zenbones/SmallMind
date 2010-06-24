package org.smallmind.quorum.cache.indigenous;

import org.smallmind.quorum.cache.CacheException;

public class CacheBuilder {

   public static <K, V> AbstractCache buildLeastRecentlyUsedCache (String cacheName, TimestampedCacheSource<K, V> cacheSource, int limit)
      throws CacheException {

      return new OrderedCache<TimestampedCacheMetaData, K, V, TimestampOrderedCacheEntry<V>>(cacheName, cacheSource, new SizeLimitedCacheAccumulator<TimestampedCacheMetaData, K, TimestampOrderedCacheEntry<V>>(limit), new TimestampedCacheMetaDataComparator());
   }

   public static <K, V> AbstractCache buildLeastRecentlyUsedCache (String cacheName, TimestampedCacheSource<K, V> cacheSource, int limit, int timeToLiveSeconds)
      throws CacheException {

      return new OrderedCache<TimestampedCacheMetaData, K, V, TimestampOrderedCacheEntry<V>>(cacheName, cacheSource, new SizeLimitedCacheAccumulator<TimestampedCacheMetaData, K, TimestampOrderedCacheEntry<V>>(limit), new TimestampedCacheMetaDataComparator(), new TimeToLiveCacheExpirationPolicy<TimestampOrderedCacheEntry<V>>(timeToLiveSeconds));
   }
}
