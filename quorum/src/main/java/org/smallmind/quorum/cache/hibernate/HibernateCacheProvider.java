package org.smallmind.quorum.cache.hibernate;

import java.util.HashMap;
import java.util.Properties;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.Timestamper;

public class HibernateCacheProvider implements CacheProvider {

   private HashMap<String, Cache> cacheMap;
   private HibernateCacheFactory hibernateCacheFactory;
   private int limit;
   private int timeToLiveSeconds;

   public HibernateCacheProvider () {

      cacheMap = new HashMap<String, Cache>();
   }

   public void setCacheImplementationProvider (HibernateCacheFactory hibernateCacheFactory) {

      this.hibernateCacheFactory = hibernateCacheFactory;
   }

   public void setLimit (int limit) {

      this.limit = limit;
   }

   public void setTimeToLiveSeconds (int timeToLiveSeconds) {

      this.timeToLiveSeconds = timeToLiveSeconds;
   }

   public synchronized void addCache (Cache cache) {

      cacheMap.put(cache.getRegionName(), cache);
   }

   public synchronized Cache buildCache (String region, Properties properties)
      throws CacheException {

      Cache cache;

      if ((cache = cacheMap.get(region)) == null) {
         cacheMap.put(region, cache = new HibernateCache(hibernateCacheFactory, region, limit, timeToLiveSeconds));
      }

      return cache;
   }

   public long nextTimestamp () {

      return Timestamper.next();
   }

   public void start (Properties properties)
      throws CacheException {
   }

   public synchronized void stop () {

      cacheMap.clear();
   }

   public boolean isMinimalPutsEnabledByDefault () {

      return false;
   }
}
