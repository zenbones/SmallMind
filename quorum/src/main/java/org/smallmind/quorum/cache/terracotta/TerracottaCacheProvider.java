package org.smallmind.quorum.cache.terracotta;

import org.smallmind.quorum.cache.ExternallyLockedCache;
import org.smallmind.quorum.cache.ExternallyLockedCacheProvider;
import org.terracotta.cache.CacheConfig;
import org.terracotta.cache.CacheConfigFactory;

public class TerracottaCacheProvider implements ExternallyLockedCacheProvider {

   private Boolean orphanEvictionEnabled;
   private Integer targetMaxInMemoryCount;
   private Integer targetMaxTotalCount;
   private Integer maxTTISeconds;
   private Integer maxTTLSeconds;
   private Integer orphanEvictionPeriod;
   private long externalLockTimeout = 0;
   private int concurrencyLevel = 128;

   public void setTargetMaxTotalCount (Integer targetMaxTotalCount) {

      this.targetMaxTotalCount = targetMaxTotalCount;
   }

   public void setTargetMaxInMemoryCount (Integer targetMaxInMemoryCount) {

      this.targetMaxInMemoryCount = targetMaxInMemoryCount;
   }

   public void setMaxTTLSeconds (Integer maxTTLSeconds) {

      this.maxTTLSeconds = maxTTLSeconds;
   }

   public void setMaxTTISeconds (Integer maxTTISeconds) {

      this.maxTTISeconds = maxTTISeconds;
   }

   public void setOrphanEvictionEnabled (Boolean orphanEvictionEnabled) {

      this.orphanEvictionEnabled = orphanEvictionEnabled;
   }

   public void setOrphanEvictionPeriod (Integer orphanEvictionPeriod) {

      this.orphanEvictionPeriod = orphanEvictionPeriod;
   }

   public void setConcurrencyLevel (int concurrencyLevel) {

      this.concurrencyLevel = concurrencyLevel;
   }

   public void setExternalLockTimeout (long externalLockTimeout) {

      this.externalLockTimeout = externalLockTimeout;
   }

   public <K, V> ExternallyLockedCache<K, V> createExternallyLockedCache (String name) {

      CacheConfig cacheConfig = CacheConfigFactory.newConfig();

      if (targetMaxTotalCount != null) {
         cacheConfig.setTargetMaxTotalCount(targetMaxTotalCount);
      }
      if (targetMaxInMemoryCount != null) {
         cacheConfig.setTargetMaxInMemoryCount(targetMaxInMemoryCount);
      }
      if (maxTTLSeconds != null) {
         cacheConfig.setMaxTTLSeconds(maxTTLSeconds);
      }
      if (maxTTISeconds != null) {
         cacheConfig.setMaxTTISeconds(maxTTISeconds);
      }
      if (orphanEvictionEnabled != null) {
         cacheConfig.setOrphanEvictionEnabled(orphanEvictionEnabled);
      }
      if (orphanEvictionPeriod != null) {
         cacheConfig.setOrphanEvictionPeriod(orphanEvictionPeriod);
      }

      cacheConfig.setName(name);

      return new TerracottaCache<K, V>(cacheConfig, concurrencyLevel, externalLockTimeout);
   }
}
