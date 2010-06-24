package org.smallmind.quorum.cache;

public interface ExternallyLockedCache<K, V> {

  public abstract String getCacheName ();

  public abstract int size ();

  public abstract V get (KeyLock keyLock, K key, Object... parameters);

  public abstract V remove (KeyLock keyLock, K key);

  public abstract V put (KeyLock keyLock, K key, V value);

  public abstract V putIfAbsent (KeyLock keyLock, K key, V value);

  public abstract boolean exists (KeyLock keyLock, K key);

  public abstract void clear ();

  public abstract boolean isClosed ();

  public abstract void close ();

  public abstract long getExternalLockTimeout ();

  public abstract KeyLock lock (KeyLock keyLock, K key);

  public abstract void unlock (KeyLock keyLock, K key);

  public abstract <R> R executeLockedCallback (KeyLock keyLock, LockedCallback<K, R> callback);
}