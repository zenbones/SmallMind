package org.smallmind.quorum.cache.indigenous;

import org.smallmind.nutsnbolts.util.UniqueId;

public class TimestampedCacheMetaData implements CacheMetaData {

   private long creationTimepstamp;
   private long lastAccessTimestamp = 0;
   private UniqueId uniqueId;

   public TimestampedCacheMetaData () {

      creationTimepstamp = System.currentTimeMillis();
      uniqueId = UniqueId.newInstance();
   }

   public UniqueId getUniqueId () {

      return uniqueId;
   }

   public long getCreationTimestamp () {

      return creationTimepstamp;
   }

   public long getLastAccessTimestamp () {

      return (lastAccessTimestamp == 0) ? creationTimepstamp : lastAccessTimestamp;
   }

   public boolean willUpdate () {

      return true;
   }

   public void update () {

      lastAccessTimestamp = System.currentTimeMillis();
   }
}
