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
package org.smallmind.persistence.cache.terracotta;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.nutsnbolts.util.MagicHash;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.CacheDomain;
import org.smallmind.persistence.cache.CacheOperationException;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class AbstractTerracottaCacheDomain<I extends Comparable<I>, D extends Durable<I>> implements CacheDomain<I, D> {

   private final ReentrantReadWriteLock[] stripeLocks;

   public AbstractTerracottaCacheDomain () {

      this(16);
   }

   public AbstractTerracottaCacheDomain (int concurrencyLevel) {

      if ((concurrencyLevel <= 0) || (concurrencyLevel % 2 != 0)) {
         throw new CacheOperationException("Concurrency level(%d) must be > 0 and an even power of 2", concurrencyLevel);
      }

      stripeLocks = new ReentrantReadWriteLock[concurrencyLevel];

      for (int count = 0; count < stripeLocks.length; count++) {
         stripeLocks[count] = new ReentrantReadWriteLock();
      }
   }

   public void readLock (Class<D> managedClass, I... ids) {

      for (I id : ids) {
         stripeLocks[Math.abs(MagicHash.rehash(id.hashCode()) % stripeLocks.length)].readLock().lock();
      }
   }

   public void readUnlock (Class<D> managedClass, I... ids) {

      for (I id : ids) {
         stripeLocks[Math.abs(MagicHash.rehash(id.hashCode()) % stripeLocks.length)].readLock().unlock();
      }
   }

   public void writeLock (Class<D> managedClass, I... ids) {

      for (I id : ids) {
         stripeLocks[Math.abs(MagicHash.rehash(id.hashCode()) % stripeLocks.length)].writeLock().lock();
      }
   }

   public void writeUnlock (Class<D> managedClass, I... ids) {

      for (I id : ids) {
         stripeLocks[Math.abs(MagicHash.rehash(id.hashCode()) % stripeLocks.length)].writeLock().unlock();
      }
   }
}
