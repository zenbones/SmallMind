package org.smallmind.quorum.cache.hibernate;

import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.LockingCache;

public interface HibernateCacheFactory {

   public abstract LockingCache<Object, Object> createCache (String regionName, int limit, int timeToLiveSeconds)
      throws CacheException;
}
