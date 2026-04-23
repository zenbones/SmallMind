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

/**
 * Collects and exposes garbage-collection statistics for the young and old (tenured) heap
 * generations by inspecting the JVM's {@link GarbageCollectorMXBean} instances at
 * construction time.
 *
 * <p>Known collector names for each generation are matched against the names returned by
 * {@link GarbageCollectorMXBean#getName()} to classify collectors as either young or old.
 * Collectors that are not recognised are ignored. When no collector is found for a generation
 * the {@code GarbageStatistics.NO_GARBAGE_STATISTICS} sentinel is used, which always returns
 * zero for both count and time.
 *
 * <p>Recognised young-generation collectors: PS Scavenge, ParNew, G1 Young Generation,
 * Shenandoah Pauses.
 * <p>Recognised old-generation collectors: PS MarkSweep, ConcurrentMarkSweep, G1 Old
 * Generation, Shenandoah Cycles, ZGC.
 */
public class GarbageFacts {

  /**
   * Statistics for the young-generation garbage collector, or the no-op sentinel when none
   * was identified.
   */
  private final GarbageStatistics youngGarbageStatistics;

  /**
   * Statistics for the old-generation garbage collector, or the no-op sentinel when none was
   * identified.
   */
  private final GarbageStatistics oldGarbageStatistics;

  /**
   * Inspects all available {@link GarbageCollectorMXBean} instances and initialises statistics
   * adapters for the young and old generations.
   */
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

  /**
   * Internal abstraction over a {@link GarbageCollectorMXBean} that provides default
   * zero-returning implementations for environments where a given collector is unavailable.
   */
  private interface GarbageStatistics {

    /**
     * Sentinel instance that always returns zero for both collection count and time; used when
     * no matching collector is found for a generation.
     */
    GarbageStatistics NO_GARBAGE_STATISTICS = new GarbageStatistics() {

    };

    /**
     * Creates a {@link GarbageStatistics} adapter that delegates directly to the supplied
     * {@link GarbageCollectorMXBean}.
     *
     * @param mxBean the GC MXBean to wrap; must not be {@code null}
     * @return a {@link GarbageStatistics} that reads live values from {@code mxBean}
     */
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

    /**
     * Returns the total number of collections performed by this collector.
     *
     * @return collection count, or {@code 0} when no collector is available
     */
    default long getCollectionCount () {

      return 0;
    }

    /**
     * Returns the approximate total elapsed time spent in collection by this collector.
     *
     * @return collection time in milliseconds, or {@code 0} when no collector is available
     */
    default long getCollectionTime () {

      return 0;
    }
  }

  /**
   * Returns the total elapsed time spent in young-generation garbage collection.
   *
   * @return young-generation collection time in milliseconds, or {@code 0} when unavailable
   */
  public long getYoungCollectionTime () {

    return youngGarbageStatistics.getCollectionTime();
  }

  /**
   * Returns the total number of young-generation garbage collections performed.
   *
   * @return young-generation collection count, or {@code 0} when unavailable
   */
  public long getYoungCollectionCount () {

    return youngGarbageStatistics.getCollectionCount();
  }

  /**
   * Returns the total elapsed time spent in old-generation garbage collection.
   *
   * @return old-generation collection time in milliseconds, or {@code 0} when unavailable
   */
  public long getOldCollectionTime () {

    return oldGarbageStatistics.getCollectionTime();
  }

  /**
   * Returns the total number of old-generation garbage collections performed.
   *
   * @return old-generation collection count, or {@code 0} when unavailable
   */
  public long getOldCollectionCount () {

    return oldGarbageStatistics.getCollectionCount();
  }
}
