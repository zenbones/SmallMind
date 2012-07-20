/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
