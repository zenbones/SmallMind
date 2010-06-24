package org.smallmind.quorum.cache.indigenous;

public class TimeToLiveCacheExpirationPolicy<E extends AbstractTimestampOrderedCacheEntry<?>> implements CacheExpirationPolicy<E> {

   private int timeToLiveSeconds;
   private int timerTickSeconds;

   public TimeToLiveCacheExpirationPolicy (int timeToLiveSeconds) {

      this(timeToLiveSeconds, 0);
   }

   public TimeToLiveCacheExpirationPolicy (int timeToLiveSeconds, int timerTickSeconds) {

      this.timeToLiveSeconds = timeToLiveSeconds;
      this.timerTickSeconds = timerTickSeconds;
   }

   public int getTimerTickSeconds () {

      return timerTickSeconds;
   }

   public boolean isStale (E cacheEntry) {

      return (cacheEntry.getCacheMetaData().getLastAccessTimestamp() - cacheEntry.getCacheMetaData().getCreationTimestamp()) > (timeToLiveSeconds * 1000);
   }
}
