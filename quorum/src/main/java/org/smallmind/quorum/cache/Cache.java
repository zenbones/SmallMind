package org.smallmind.quorum.cache;

public interface Cache<K, V> {

   public abstract int size ();

   public abstract String getCacheName ();

   public abstract V get (K key, Object... parameters);

   public abstract V remove (K key);

   public abstract V put (K key, V value);

   public abstract V putIfAbsent (K key, V value);

   public abstract boolean exists (K key);

   public abstract void clear ();

   public abstract boolean isClosed ();

   public abstract void close ();
}
