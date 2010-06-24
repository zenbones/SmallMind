package org.smallmind.quorum.cache.terracotta;

import java.util.HashMap;
import org.smallmind.quorum.cache.ExternallyLockedCache;
import org.smallmind.quorum.cache.LockingCache;
import org.smallmind.quorum.cache.LockingCacheEnforcer;
import org.smallmind.quorum.cache.LockingCacheManager;
import org.terracotta.modules.annotations.AutolockWrite;
import org.terracotta.modules.annotations.Root;

public class TerracottaCacheManager<K, V> implements LockingCacheManager<K, V> {

   @Root
   private final HashMap<String, HashMap<String, ExternallyLockedCache<K, V>>> level3Map = new HashMap<String, HashMap<String, ExternallyLockedCache<K, V>>>();

   private TerracottaCacheProvider cacheProvider;
   private String instance;

   public TerracottaCacheManager (TerracottaCacheProvider cacheProvider, String instance) {

      this.cacheProvider = cacheProvider;
      this.instance = instance;
   }

   @AutolockWrite
   public LockingCache<K, V> getLockingCache (String name) {

      HashMap<String, ExternallyLockedCache<K, V>> instanceMap;
      ExternallyLockedCache<K, V> cache;

      synchronized (level3Map) {

         if ((instanceMap = level3Map.get(instance)) == null) {
            level3Map.put(instance, instanceMap = new HashMap<String, ExternallyLockedCache<K, V>>());
         }

         if ((cache = instanceMap.get(name)) == null) {
            instanceMap.put(name, cache = cacheProvider.createExternallyLockedCache(name));
         }
      }

      return new LockingCacheEnforcer<K, V>(cache);
   }
}
