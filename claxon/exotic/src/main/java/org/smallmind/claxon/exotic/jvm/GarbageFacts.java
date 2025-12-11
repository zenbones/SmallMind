/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.claxon.exotic.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class GarbageFacts {

  private final GarbageStatistics youngGarbageStatistics;
  private final GarbageStatistics oldGarbageStatistics;

  public GarbageFacts () {

    List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    GarbageCollectorMXBean youngGarbageCollectorMXBean = null;
    GarbageCollectorMXBean oldGarbageCollectorMXBean = null;

    for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
      switch (garbageCollectorMXBean.getName()) {
        case "PS Scavenge", "ParNew", "G1 Young Generation", "Shenandoah Pauses" -> youngGarbageCollectorMXBean = garbageCollectorMXBean;
        case "PS MarkSweep", "ConcurrentMarkSweep", "G1 Old Generation", "Shenandoah Cycles", "ZGC" -> oldGarbageCollectorMXBean = garbageCollectorMXBean;
      }
    }

    youngGarbageStatistics = (youngGarbageCollectorMXBean == null) ? GarbageStatistics.NO_GARBAGE_STATISTICS : GarbageStatistics.from(youngGarbageCollectorMXBean);
    oldGarbageStatistics = (oldGarbageCollectorMXBean) == null ? GarbageStatistics.NO_GARBAGE_STATISTICS : GarbageStatistics.from(oldGarbageCollectorMXBean);
  }

  private interface GarbageStatistics {

    GarbageStatistics NO_GARBAGE_STATISTICS = new GarbageStatistics() {

    };

    static GarbageStatistics from (GarbageCollectorMXBean mxBean) {

      return new GarbageStatistics() {

        @Override
        public long getCollectionCount () {

          return mxBean.getCollectionCount();
        }

        @Override
        public long getCollectionTime () {

          return mxBean.getCollectionTime();
        }
      };
    }

    default long getCollectionCount () {

      return 0;
    }

    default long getCollectionTime () {

      return 0;
    }
  }

  public long getYoungCollectionTime () {

    return youngGarbageStatistics.getCollectionTime();
  }

  public long getYoungCollectionCount () {

    return youngGarbageStatistics.getCollectionCount();
  }

  public long getOldCollectionTime () {

    return oldGarbageStatistics.getCollectionTime();
  }

  public long getOldCollectionCount () {

    return oldGarbageStatistics.getCollectionCount();
  }
}
