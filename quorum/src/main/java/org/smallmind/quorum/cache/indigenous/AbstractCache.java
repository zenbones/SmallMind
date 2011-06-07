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
package org.smallmind.quorum.cache.indigenous;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.ExternallyLockedCache;
import org.smallmind.quorum.cache.KeyLock;
import org.smallmind.quorum.cache.LockManager;
import org.smallmind.quorum.cache.StripeLockFactory;

public abstract class AbstractCache<K, V, E extends CacheEntry<V>> extends LockManager<K, V> implements ExternallyLockedCache<K, V> {

   private transient ExpirationTimer<K> expirationTimer;

   private InternalHashMap<K, E> cacheReferenceMap;
   private CacheSource<K, V, E> cacheSource;
   private CacheExpirationPolicy<E> cacheExpirationPolicy;
   private String cacheName;
   private AtomicBoolean closed = new AtomicBoolean(false);

   public AbstractCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheExpirationPolicy<E> cacheExpirationPolicy, int initialCapacity, float loadFactor, int concurrencyLevel, long externalLockTimeout)
      throws CacheException {

      super(StripeLockFactory.createStripeLockArray(concurrencyLevel), externalLockTimeout);

      this.cacheName = cacheName;
      this.cacheSource = cacheSource;
      this.cacheExpirationPolicy = cacheExpirationPolicy;

      cacheReferenceMap = new InternalHashMap<K, E>(getStripeLockArray(), initialCapacity, loadFactor);

      if ((cacheExpirationPolicy != null) && (cacheExpirationPolicy.getTimerTickSeconds() > 0)) {

         Thread expirationTimerThread;

         expirationTimer = new ExpirationTimer<K>(this, cacheExpirationPolicy.getTimerTickSeconds());
         expirationTimerThread = new Thread(expirationTimer);
         expirationTimerThread.setDaemon(true);
         expirationTimerThread.start();
      }
   }

   public abstract V get (KeyLock keyLock, K key, Object... parameters)
      throws CacheException;

   public abstract V remove (KeyLock keyLock, K key)
      throws CacheException;

   protected Iterator<K> getKeyIterator () {

      if (closed.get()) {
         throw new IllegalStateException("The AbstractCache has been previously closed()");
      }

      return cacheReferenceMap.keyIterator();
   }

   protected E expireEntry (K key) {

      E cacheEntry;

      if ((cacheEntry = cacheReferenceMap.remove(key)) != null) {
         cacheEntry.expire();
      }

      return cacheEntry;
   }

   protected E getExistingEntry (KeyLock keyLock, K key)
      throws CacheException {

      E cacheEntry;

      if ((cacheEntry = retrieveEntry(keyLock, key)) != null) {
         cacheEntry.cacheHit();

         return cacheEntry;
      }

      return null;
   }

   protected E createNewEntry (K key, Object... parameters)
      throws CacheException {

      E cacheEntry;

      if ((cacheEntry = cacheSource.createEntry(key, parameters)) != null) {
         implantReference(new CacheReference<K, E>(key, cacheEntry));

         return cacheEntry;
      }

      return null;
   }

   protected E retrieveEntry (KeyLock keyLock, K key)
      throws CacheException {

      E cacheEntry;

      if ((cacheEntry = cacheReferenceMap.get(key)) != null) {
         if (cacheExpirationPolicy != null) {
            if (cacheExpirationPolicy.isStale(cacheEntry)) {
               remove(keyLock, key);

               return null;
            }
         }

         return cacheEntry;
      }

      return null;
   }

   protected E implantReference (CacheReference<K, E> cacheReference)
      throws CacheException {

      return cacheReferenceMap.put(cacheReference.getKey(), cacheReference.getCacheEntry());
   }

   public String getCacheName () {

      return cacheName;
   }

   public int size () {

      return cacheReferenceMap.size();
   }

   public void clear () {

      throw new UnsupportedOperationException();
   }

   public void validate (KeyLock keyLock, CacheValidationPolicy<E> cacheValidationPolicy)
      throws CacheException {

      ReentrantLock stripeLock;
      E cacheEntry;

      if (closed.get()) {
         throw new IllegalStateException("The AbstractCache has been previously closed()");
      }

      for (K key : cacheReferenceMap.keyIterator()) {
         stripeLock = lockStripe(key);
         try {
            gateKey(keyLock, key);

            if ((cacheEntry = cacheReferenceMap.get(key)) != null) {
               if (!cacheValidationPolicy.isValid(cacheEntry)) {
                  remove(keyLock, key);
               }
            }
         }
         finally {
            stripeLock.unlock();
         }
      }
   }

   public boolean exists (KeyLock keyLock, K key)
      throws CacheException {

      ReentrantLock stripeLock;

      if (closed.get()) {
         throw new IllegalStateException("The AbstractCache has been previously closed()");
      }

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         return retrieveEntry(keyLock, key) != null;
      }
      finally {
         stripeLock.unlock();
      }
   }

   public V put (KeyLock keyLock, K key, V value)
      throws CacheException {

      ReentrantLock stripeLock;
      E cacheEntry;

      if (closed.get()) {
         throw new IllegalStateException("The AbstractCache has been previously closed()");
      }

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         if ((cacheEntry = implantReference(cacheSource.wrapReference(key, value))) != null) {

            return cacheEntry.getEntry();
         }

         return null;
      }
      finally {
         stripeLock.unlock();
      }
   }

   public V putIfAbsent (KeyLock keyLock, K key, V value) {

      ReentrantLock stripeLock;
      E cacheEntry;

      if (closed.get()) {
         throw new IllegalStateException("The AbstractCache has been previously closed()");
      }

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         if ((cacheEntry = retrieveEntry(keyLock, key)) == null) {
            implantReference(cacheSource.wrapReference(key, value));

            return null;
         }

         return cacheEntry.getEntry();
      }
      finally {
         stripeLock.unlock();
      }
   }

   public boolean isClosed () {

      return closed.get();
   }

   public void close () {

      if (closed.compareAndSet(false, true)) {

         if (expirationTimer != null) {
            expirationTimer.finish();
         }
      }
   }
}