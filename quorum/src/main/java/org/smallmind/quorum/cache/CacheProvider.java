package org.smallmind.quorum.cache;

public interface CacheProvider {

   public abstract <K, V> Cache<K, V> getCache (String instance, String region);

   public abstract <K, V> Cache<K, V> getCache (String instance, String region, boolean create);

   public abstract void clearCache (String instance, String region);
}
