package org.smallmind.quorum.cache.indigenous;

import java.util.Comparator;

public class TimestampedCacheMetaDataComparator implements Comparator<TimestampedCacheMetaData> {

   public int compare (TimestampedCacheMetaData metaData1, TimestampedCacheMetaData metaData2) {

      int comparison;

      /**
       * The meta data for the cache must properly distribute based upon last access times within
       * their containing sorted map, but must not return an equal comparison unless the two objects
       * are actually identical. Therefore, if last access time fails to differentiate the meta
       * data objects being compared, we compare identity values, which should be equivalent only
       * if the meta data are the same object (equivalent by ==), in which case we can safely return
       * 0 (and this comparison is valid across diistributed JVMs).
       */
      if ((comparison = (int)(metaData1.getLastAccessTimestamp() - metaData2.getLastAccessTimestamp())) == 0) {

         return metaData1.getUniqueId().compareTo(metaData2.getUniqueId());
      }

      return comparison;
   }
}
