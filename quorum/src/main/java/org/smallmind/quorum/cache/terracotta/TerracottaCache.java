package org.smallmind.quorum.cache.terracotta;

import java.util.concurrent.locks.ReentrantLock;
import org.smallmind.quorum.cache.ExternallyLockedCache;
import org.smallmind.quorum.cache.KeyLock;
import org.smallmind.quorum.cache.LockManager;
import org.smallmind.quorum.cache.StripeLockFactory;
import org.terracotta.cache.CacheConfig;
import org.terracotta.cache.CacheConfigFactory;
import org.terracotta.cache.DistributedCache;
import org.terracotta.modules.annotations.InstrumentedClass;

@InstrumentedClass
public class TerracottaCache<K, V> extends LockManager<K> implements ExternallyLockedCache<K, V> {

   private CacheConfig cacheConfig;
   private DistributedCache<K, V> cache;

   public TerracottaCache (CacheConfig cacheConfig, int concurrencyLevel, long externalLockTimeout) {

      super(StripeLockFactory.createStripeLockArray(concurrencyLevel), externalLockTimeout);

      this.cacheConfig = cacheConfig;

      cache = CacheConfigFactory.newConfig().newCache();
   }

   public String getCacheName () {

      return cacheConfig.getName();
   }

   public int size () {

      throw new UnsupportedOperationException();
   }

   public boolean exists (KeyLock keyLock, K key) {

      ReentrantLock stripeLock;

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         return cache.containsKey(key);
      }
      finally {
         stripeLock.unlock();
      }
   }

   public V get (KeyLock keyLock, K key, Object... parameters) {

      ReentrantLock stripeLock;

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         return cache.get(key);
      }
      finally {
         stripeLock.unlock();
      }
   }

   public V put (KeyLock keyLock, K key, final V value) {

      ReentrantLock stripeLock;

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         return cache.put(key, value);
      }
      finally {
         stripeLock.unlock();
      }
   }

   public V putIfAbsent (KeyLock keyLock, K key, final V value) {

      ReentrantLock stripeLock;

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         return cache.put(key, value);
      }
      finally {
         stripeLock.unlock();
      }
   }

   public V remove (KeyLock keyLock, K key) {

      ReentrantLock stripeLock;

      stripeLock = lockStripe(key);
      try {
         gateKey(keyLock, key);

         return cache.remove(key);
      }
      finally {
         stripeLock.unlock();
      }
   }

   public void clear () {

      throw new UnsupportedOperationException();
   }

   public boolean isClosed () {

      throw new UnsupportedOperationException();
   }

   public void close () {

      cache.shutdown();
   }
}
