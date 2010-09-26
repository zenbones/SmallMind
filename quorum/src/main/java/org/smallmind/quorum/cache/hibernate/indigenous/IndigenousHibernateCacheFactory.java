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
