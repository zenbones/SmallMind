package org.smallmind.quorum.cache;

public interface LockingCache<K, V> extends Cache<K, V> {

   public abstract long getExternalLockTimeout ();

   public abstract void lock (K key);

   public abstract void unlock (K key);

   public abstract <R> R executeLockedCallback (LockedCallback<K, R> callback);
}