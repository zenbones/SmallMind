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
