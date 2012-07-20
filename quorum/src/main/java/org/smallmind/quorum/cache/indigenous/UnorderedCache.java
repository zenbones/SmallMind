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

import java.util.concurrent.locks.ReentrantLock;
import org.smallmind.quorum.cache.CacheException;
import org.smallmind.quorum.cache.KeyLock;

public class UnorderedCache<K, V, E extends CacheEntry<V>> extends AbstractCache<K, V, E> {

  public UnorderedCache (String cacheName, CacheSource<K, V, E> cacheSource)
    throws CacheException {

    this(cacheName, cacheSource, null, 16, .75F, 16, 0);
  }

  public UnorderedCache (String cacheName, CacheSource<K, V, E> cacheSource, int initialCapacity, float loadFactor, int concurrencyLevel)
    throws CacheException {

    this(cacheName, cacheSource, null, initialCapacity, loadFactor, concurrencyLevel, 0);
  }

  public UnorderedCache (String cacheName, CacheSource<K, V, E> cacheSource, int initialCapacity, float loadFactor, int concurrencyLevel, long externalLockTimeout)
    throws CacheException {

    this(cacheName, cacheSource, null, initialCapacity, loadFactor, concurrencyLevel, externalLockTimeout);
  }

  public UnorderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheExpirationPolicy<E> cacheExpirationPolicy)
    throws CacheException {

    this(cacheName, cacheSource, cacheExpirationPolicy, 16, .75F, 16, 0);
  }

  public UnorderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheExpirationPolicy<E> cacheExpirationPolicy, int initialCapacity, float loadFactor, int concurrencyLevel)
    throws CacheException {

    this(cacheName, cacheSource, cacheExpirationPolicy, initialCapacity, loadFactor, concurrencyLevel, 0);
  }

  public UnorderedCache (String cacheName, CacheSource<K, V, E> cacheSource, CacheExpirationPolicy<E> cacheExpirationPolicy, int initialCapacity, float loadFactor, int concurrencyLevel, long externalLockTimeout)
    throws CacheException {

    super(cacheName, cacheSource, cacheExpirationPolicy, initialCapacity, loadFactor, concurrencyLevel, externalLockTimeout);
  }

  public V remove (KeyLock keyLock, K key) {

    ReentrantLock stripeLock;
    E cacheEntry;

    if (isClosed()) {
      throw new IllegalStateException("The AbstractCache has been previously closed()");
    }

    stripeLock = lockStripe(key);
    try {
      gateKey(keyLock, key);
      if ((cacheEntry = expireEntry(key)) != null) {
        return cacheEntry.getEntry();
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
    E cacheEntry;

    if (isClosed()) {
      throw new IllegalStateException("The AbstractCache has been previously closed()");
    }

    stripeLock = lockStripe(key);
    try {
      gateKey(keyLock, key);

      if ((cacheEntry = getExistingEntry(keyLock, key)) != null) {
        return cacheEntry.getEntry();
      }
      else if ((cacheEntry = createNewEntry(key, parameters)) != null) {
        return cacheEntry.getEntry();
      }
      else {
        return null;
      }
    }
    finally {
      stripeLock.unlock();
    }
  }
}
