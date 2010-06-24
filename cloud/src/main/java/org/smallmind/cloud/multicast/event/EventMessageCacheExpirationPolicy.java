package org.smallmind.cloud.multicast.event;

import org.smallmind.quorum.cache.indigenous.CacheExpirationPolicy;

public class EventMessageCacheExpirationPolicy implements CacheExpirationPolicy<EventMessageCacheEntry> {

   private int timerTickSeconds;

   public EventMessageCacheExpirationPolicy (int timerTickSeconds) {

      this.timerTickSeconds = timerTickSeconds;
   }

   public int getTimerTickSeconds () {

      return timerTickSeconds;
   }

   public boolean isStale (EventMessageCacheEntry cacheEntry) {

      return ((System.currentTimeMillis() - cacheEntry.getLastAccessTime()) > 10000);
   }
}
