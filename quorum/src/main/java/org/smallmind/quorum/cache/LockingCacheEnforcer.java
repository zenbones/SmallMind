package org.smallmind.quorum.cache;

import java.util.HashMap;

public class LockingCacheEnforcer<K, V> implements LockingCache<K, V> {

  private static final InheritableThreadLocal<HashMap<Object, KeyLock>> KEY_LOCK_MAP_LOCAL = new InheritableThreadLocal<HashMap<Object, KeyLock>>() {
    @Override
    protected HashMap<Object, KeyLock> initialValue () {

      return new HashMap<Object, KeyLock>();
    }
  };

  private ExternallyLockedCache<K, V> cache;

  public LockingCacheEnforcer (ExternallyLockedCache<K, V> cache) {

    this.cache = cache;
  }

  public long getExternalLockTimeout () {

    return cache.getExternalLockTimeout();
  }

  public void lock (K key) {

    KEY_LOCK_MAP_LOCAL.get().put(key, cache.lock(KEY_LOCK_MAP_LOCAL.get().get(key), key));
  }

  public void unlock (K key) {

    cache.unlock(KEY_LOCK_MAP_LOCAL.get().remove(key), key);
  }

  public <R> R executeLockedCallback (LockedCallback<K, R> callback) {

    return cache.executeLockedCallback(KEY_LOCK_MAP_LOCAL.get().get(callback.getKey()), callback);
  }

  public String getCacheName () {

    return cache.getCacheName();
  }

  public int size () {

    return cache.size();
  }

  public V get (K key, Object... parameters) {

    return cache.get(KEY_LOCK_MAP_LOCAL.get().get(key), key, parameters);
  }

  public V remove (K key) {

    return cache.remove(KEY_LOCK_MAP_LOCAL.get().get(key), key);
  }

  public V put (K key, V value) {

    return cache.put(KEY_LOCK_MAP_LOCAL.get().get(key), key, value);
  }

  public V putIfAbsent (K key, V value) {

    return cache.putIfAbsent(KEY_LOCK_MAP_LOCAL.get().get(key), key, value);
  }

  public boolean exists (K key) {

    return cache.exists(KEY_LOCK_MAP_LOCAL.get().get(key), key);
  }

  public void clear () {

    cache.clear();
  }

  public boolean isClosed () {

    return cache.isClosed();
  }

  public void close () {

    cache.close();
  }
}
