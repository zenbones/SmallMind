package org.smallmind.quorum.cache.hibernate.indigenous;

import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.LockingCache;
import org.smallmind.quorum.cache.hibernate.HibernateCacheFactory;
import org.smallmind.quorum.cache.indigenous.TimestampedCacheMetaDataComparator;

public class IndigenousHibernateCacheFactory implements HibernateCacheFactory {

   private static HibernateTimestampedCacheSource HIBERNATE_TIMESTAMPED_CACHE_SOURCE = new HibernateTimestampedCacheSource();
   private static TimestampedCacheMetaDataComparator TIMESTAMPED_CACHE_META_DATA_COMPARATOR = new TimestampedCacheMetaDataComparator();

   public LockingCache<Object, Object> createCache (String regionName, int limit, int timeToLiveSeconds)
      throws CacheException {

      //TODO:
//      return new OrderedCache<TimestampedCacheMetaData, Object, Object, TimestampOrderedCacheEntry<Object>>(regionName, HIBERNATE_TIMESTAMPED_CACHE_SOURCE, new SizeLimitedCacheAccumulator<TimestampedCacheMetaData, Object, TimestampOrderedCacheEntry<Object>>(limit), TIMESTAMPED_CACHE_META_DATA_COMPARATOR, (timeToLiveSeconds > 0) ? new TimeToLiveCacheExpirationPolicy<TimestampOrderedCacheEntry<Object>>(timeToLiveSeconds, 60) : null);
      return null;
   }
}
