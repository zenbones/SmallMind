/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.quorum.cache.indigenous;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.KeyLock;

public class OrderedCache<D extends CacheMetaData, K, V, E extends OrderedCacheEntry<D, V>> extends AbstractCache<K, V, E> {

   private ConcurrentSkipListMap<D, K> cacheKeyMap;
   private CacheAccumulator<D, K, E> cacheAccumulator;

   public OrderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheAccumulator<D, K, E> cacheAccumulator, Comparator<D> metaDataComparator)
      throws CacheException {

      this(cacheName, cacheSource, cacheAccumulator, metaDataComparator, null, 16, .75F, 16, 0);
   }

   public OrderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheAccumulator<D, K, E> cacheAccumulator, Comparator<D> metaDataComparator, int initialCapacity, float loadFactor, int concurrencyLevel)
      throws CacheException {

      this(cacheName, cacheSource, cacheAccumulator, metaDataComparator, null, initialCapacity, loadFactor, concurrencyLevel, 0);
   }

   public OrderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheAccumulator<D, K, E> cacheAccumulator, Comparator<D> metaDataComparator, int initialCapacity, float loadFactor, int concurrencyLevel, long externalLockTimeout)
      throws CacheException {

      this(cacheName, cacheSource, cacheAccumulator, metaDataComparator, null, initialCapacity, loadFactor, concurrencyLevel, externalLockTimeout);
   }

   public OrderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheAccumulator<D, K, E> cacheAccumulator, Comparator<D> metaDataComparator, CacheExpirationPolicy<E> cacheExpirationPolicy)
      throws CacheException {

      this(cacheName, cacheSource, cacheAccumulator, metaDataComparator, cacheExpirationPolicy, 16, .75F, 16, 0);
   }

   public OrderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheAccumulator<D, K, E> cacheAccumulator, Comparator<D> metaDataComparator, CacheExpirationPolicy<E> cacheExpirationPolicy, int initialCapacity, float loadFactor, int concurrencyLevel)
      throws CacheException {

      this(cacheName, cacheSource, cacheAccumulator, metaDataComparator, cacheExpirationPolicy, initialCapacity, loadFactor, concurrencyLevel, 0);
   }

   public OrderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheAccumulator<D, K, E> cacheAccumulator, Comparator<D> metaDataComparator, CacheExpirationPolicy<E> cacheExpirationPolicy, int initialCapacity, float loadFactor, int concurrencyLevel, long externalLockTimeout)
      throws CacheException {

      super(cacheName, cacheSource, cacheExpirationPolicy, initialCapacity, loadFactor, concurrencyLevel, externalLockTimeout);

      this.cacheAccumulator = cacheAccumulator;

      cacheKeyMap = new ConcurrentSkipListMap<D, K>(metaDataComparator);
   }

   public V remove (KeyLock keyLock, K key)
      throws CacheException {

      ReentrantLock stripeLock;
      E orderedCacheEntry;

      if (isClosed()) {
         throw new IllegalStateException("The AbstractCache has been previously closed()");
      }

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         if ((orderedCacheEntry = expireEntry(key)) != null) {
            if (cacheKeyMap.remove(orderedCacheEntry.getCacheMetaData()) == null) {
               throw new CacheException("Attempt to remove key(%s) succeeded, but its related metadata was not present in the AbstractCache", key);
            }
            cacheAccumulator.remove(orderedCacheEntry.getCacheMetaData());

            return orderedCacheEntry.getEntry();
         }

         return null;
      }
      finally {
         stripeLock.unlock();
      }
   }

   public V get (KeyLock keyLock, K key, Object... parameters)
      throws CacheException {

      ReentrantLock stripeLock;
      E orderedCacheEntry;
      D cacheMetaData;

      if (isClosed()) {
         throw new IllegalStateException("The AbstractCache has been previously closed()");
      }

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         if ((orderedCacheEntry = getExistingEntry(keyLock, key)) != null) {
            if ((cacheMetaData = orderedCacheEntry.getCacheMetaData()).willUpdate()) {
               cacheKeyMap.remove(cacheMetaData);
               cacheMetaData.update();
               cacheKeyMap.put(cacheMetaData, key);
            }

            return orderedCacheEntry.getEntry();
         }
         else if ((orderedCacheEntry = createNewEntry(key, parameters)) != null) {

            return orderedCacheEntry.getEntry();
         }
         else {

            return null;
         }
      }
      finally {
         stripeLock.unlock();
      }
   }

   protected E implantReference (CacheReference<K, E> cacheReference)
      throws CacheException {

      E prevCacheEntry;
      Map.Entry<D, K> overLimitEntry;

      if ((prevCacheEntry = super.implantReference(cacheReference)) != null) {
         if (cacheKeyMap.remove(prevCacheEntry.getCacheMetaData()) == null) {
            throw new CacheException("Attempt to replace key(%s) succeeded, but the previous entry's related metadata was not present in the AbstractCache", cacheReference.getKey());
         }
         cacheAccumulator.remove(prevCacheEntry.getCacheMetaData());
      }

      cacheKeyMap.put(cacheReference.getCacheEntry().getCacheMetaData(), cacheReference.getKey());
      cacheAccumulator.add(cacheReference.getCacheEntry().getCacheMetaData());

      while (cacheAccumulator.isOverLimit() && (!cacheKeyMap.isEmpty())) {
         overLimitEntry = cacheKeyMap.firstEntry();
         if (cacheReference.getKey().equals(overLimitEntry.getValue())) {
            throw new CacheException("Attempt by the Accumulator to remove the same key(%s) being inserted - you may want to look at either your CacheMetaData implementation or its Comparator", overLimitEntry.getKey());
         }

         //TODO:
//         remove(overLimitEntry.getValue());
      }

      return prevCacheEntry;
   }
}
