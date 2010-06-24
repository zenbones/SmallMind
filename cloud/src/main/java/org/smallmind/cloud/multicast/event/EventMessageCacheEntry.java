package org.smallmind.cloud.multicast.event;

import org.smallmind.quorum.cache.indigenous.CacheEntry;

public class EventMessageCacheEntry implements CacheEntry<EventMessageMold> {

   private EventMessageMold messageMold;
   private long lastAccessTime;

   public EventMessageCacheEntry (EventMessageMold messageMold) {

      this.messageMold = messageMold;

      lastAccessTime = System.currentTimeMillis();
   }

   public synchronized long getLastAccessTime () {

      return lastAccessTime;
   }

   public EventMessageMold getEntry () {

      return messageMold;
   }

   public synchronized void cacheHit () {

      lastAccessTime = System.currentTimeMillis();
   }

   public void expire () {
   }

   public void close () {
   }

}
