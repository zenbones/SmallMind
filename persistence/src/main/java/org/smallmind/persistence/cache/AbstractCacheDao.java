/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.cache;

import org.smallmind.persistence.Durable;
import org.smallmind.quorum.cache.Cache;

public abstract class AbstractCacheDao<I extends Comparable<I>, D extends Durable<I>> implements CacheDao<I, D> {

   private CacheDomain<I, D> cacheDomain;

   public AbstractCacheDao (CacheDomain<I, D> cacheDomain) {

      this.cacheDomain = cacheDomain;
   }

   public void lookupLock () {

      cacheDomain.lookupLock();
   }

   public void lookupUnlock () {

      cacheDomain.lookupUnlock();
   }

   public void updateLock () {

      cacheDomain.updateLock();
   }

   public void updateUnlock () {

      cacheDomain.updateUnlock();
   }

   public void readLock (Class<D> managedClass, I id) {

      cacheDomain.readLock(managedClass, id);
   }

   public void readUnlock (Class<D> managedClass, I id) {

      cacheDomain.readUnlock(managedClass, id);
   }

   public void writeLock (Class<D> managedClass, I id) {

      cacheDomain.writeLock(managedClass, id);
   }

   public void writeUnlock (Class<D> managedClass, I id) {

      cacheDomain.writeUnlock(managedClass, id);
   }

   public abstract D acquire (Class<D> durableClass, I ids);

   public String getStatisticsSource () {

      return cacheDomain.getStatisticsSource();
   }

   public Cache<String, D> getInstanceCache (Class<D> durableClass) {

      return cacheDomain.getInstanceCache(durableClass);
   }

   public Cache<String, DurableVector<I, D>> getVectorCache (Class<D> durableClass) {

      return cacheDomain.getVectorCache(durableClass);
   }
}