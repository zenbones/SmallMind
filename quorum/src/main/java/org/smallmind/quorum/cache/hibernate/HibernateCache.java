/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
