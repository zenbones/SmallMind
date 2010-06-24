package org.smallmind.quorum.cache.hibernate;

import java.util.Map;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.Timestamper;

public class HibernateCache implements Cache {

   private org.smallmind.quorum.cache.LockingCache<Object, Object> innerCache;

   public HibernateCache (HibernateCacheFactory hibernateCacheFactory, String regionName, int limit, int timeToLiveSeconds)
      throws CacheException {

      try {
         innerCache = hibernateCacheFactory.createCache(regionName, limit, timeToLiveSeconds);
      }
      catch (org.smallmind.quorum.cache.CacheException innerCacheException) {
         throw new CacheException(innerCacheException);
      }
   }

   public Object read (Object o)
      throws CacheException {

      try {
         return innerCache.get(o);
      }
      catch (org.smallmind.quorum.cache.CacheException innerCacheException) {
         throw new CacheException(innerCacheException);
      }
   }

   public Object get (Object o)
      throws CacheException {

      try {
         return innerCache.get(o);
      }
      catch (org.smallmind.quorum.cache.CacheException innerCacheException) {
         throw new CacheException(innerCacheException);
      }
   }

   public void put (Object o, Object o1)
      throws CacheException {

      try {
         innerCache.put(o, o1);
      }
      catch (org.smallmind.quorum.cache.CacheException innerCacheException) {
         throw new CacheException(innerCacheException);
      }
   }

   public void update (Object o, Object o1)
      throws CacheException {

      try {
         innerCache.put(o, o1);
      }
      catch (org.smallmind.quorum.cache.CacheException innerCacheException) {
         throw new CacheException(innerCacheException);
      }
   }

   public void remove (Object o)
      throws CacheException {

      try {
         innerCache.remove(o);
      }
      catch (org.smallmind.quorum.cache.CacheException innerCacheException) {
         throw new CacheException(innerCacheException);
      }
   }

   public void clear ()
      throws CacheException {

      innerCache.clear();
   }

   public void destroy ()
      throws CacheException {

      innerCache.close();
   }

   public void lock (Object o)
      throws CacheException {

      innerCache.lock(o);
   }

   public void unlock (Object o)
      throws CacheException {

      innerCache.unlock(o);
   }

   public long nextTimestamp () {

      return Timestamper.next();
   }

   public int getTimeout () {

      return (int)innerCache.getExternalLockTimeout();
   }

   public String getRegionName () {

      return innerCache.getCacheName();
   }

   public long getSizeInMemory () {

      throw new UnsupportedOperationException();
   }

   public long getElementCountInMemory () {

      return innerCache.size();
   }

   public long getElementCountOnDisk () {

      return 0;
   }

   public Map toMap () {

      throw new UnsupportedOperationException();
   }
}
