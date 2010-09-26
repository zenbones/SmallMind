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
