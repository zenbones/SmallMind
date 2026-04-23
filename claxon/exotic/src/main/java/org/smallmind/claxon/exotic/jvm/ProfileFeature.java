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

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.feature.Feature;

/**
 * Claxon {@link Feature} that reports a comprehensive set of JVM memory and garbage-collection
 * profile metrics on each recording cycle.
 *
 * <p>The following quantities are emitted per cycle:
 * <ul>
 *   <li>{@code totalMemorySize} — total physical memory of the host OS in bytes</li>
 *   <li>{@code freeMemorySize} — free physical memory of the host OS in bytes</li>
 *   <li>{@code userMemoryPercent} — percentage of physical memory in use</li>
 *   <li>{@code heapMemoryMax} — JVM maximum heap size in bytes</li>
 *   <li>{@code heapMemoryUsed} — JVM heap memory currently used in bytes</li>
 *   <li>{@code processCPUTime} — JVM process CPU time in nanoseconds</li>
 *   <li>{@code compilationTime} — total JIT compilation time in milliseconds</li>
 *   <li>{@code youngGenerationHeapSize} — estimated young-generation heap size in bytes
 *       (heap used minus tenured max)</li>
 *   <li>{@code youngCollectionCount} — young-generation GC collection count</li>
 *   <li>{@code youngCollectionTime} — young-generation GC time in milliseconds</li>
 *   <li>{@code oldCollectionCount} — old-generation GC collection count</li>
 *   <li>{@code oldCollectionTime} — old-generation GC time in milliseconds</li>
 *   <li>{@code edenMemoryUsed} — eden space used memory in bytes</li>
 *   <li>{@code survivorMemoryUsed} — survivor space used memory in bytes</li>
 *   <li>{@code tenuredMemoryUsed} — tenured space used memory in bytes</li>
 * </ul>
 *
 * <p>An optional minimum recording delay can be configured to throttle emissions; when the
 * delay has not elapsed since the last successful recording, {@link #record()} returns
 * {@code null}. An optional hostname tag can be automatically prepended to the tag list.
 */
public class ProfileFeature implements Feature {

  /**
   * The combined tag array used when this feature is recorded, optionally including a
   * {@code host} tag with the local hostname.
   */
  private final Tag[] tags;

  /**
   * The meter name under which quantities are reported.
   */
  private final String name;

  /**
   * The minimum number of milliseconds that must elapse between successive emissions, or
   * {@code null} to disable throttling.
   */
  private final Long minimumRecordingDelayMilliseconds;

  /**
   * The wall-clock timestamp (in milliseconds) of the most recent successful
   * {@link #record()} call, or {@code -1} if {@link #record()} has never been called.
   */
  private long lastRecordingTimestamp = -1;

  /**
   * Constructs a JVM profile feature with optional hostname tagging and emission throttling.
   *
   * <p>When {@code addHostNameTag} is {@code true}, the local hostname is resolved via
   * {@link InetAddress#getLocalHost()} and prepended to the tag list as {@code host=<name>}.
   *
   * @param name                              the meter name under which quantities are reported
   * @param minimumRecordingDelayMilliseconds the minimum number of milliseconds between
   *                                          successive emissions; {@code null} disables
   *                                          throttling
   * @param addHostNameTag                    {@code true} to prepend a {@code host} tag
   *                                          containing the local hostname
   * @param tags                              additional tags to attach to every emission; may
   *                                          be empty
   * @throws UnknownHostException if {@code addHostNameTag} is {@code true} and the local
   *                              hostname cannot be resolved
   */
  public ProfileFeature (String name, Long minimumRecordingDelayMilliseconds, boolean addHostNameTag, Tag... tags)
    throws UnknownHostException {

    this.name = name;
    this.minimumRecordingDelayMilliseconds = minimumRecordingDelayMilliseconds;

    if (!addHostNameTag) {
      this.tags = tags;
    } else {

      InetAddress localHost = InetAddress.getLocalHost();
      String hostName = localHost.getHostName();

      if ((tags == null) || (tags.length == 0)) {
        this.tags = new Tag[] {new Tag("host", hostName)};
      } else {
        this.tags = new Tag[tags.length + 1];

        this.tags[0] = new Tag("host", hostName);
        System.arraycopy(tags, 0, this.tags, 1, tags.length);
      }
    }
  }

  /**
   * Returns the meter name under which this feature's quantities are reported.
   *
   * @return the meter name supplied at construction time
   */
  @Override
  public String getName () {

    return name;
  }

  /**
   * Returns the tags attached to every emission from this feature, potentially including the
   * automatically added {@code host} tag.
   *
   * @return the tag array; never {@code null} but may be empty
   */
  @Override
  public Tag[] getTags () {

    return tags;
  }

  /**
   * Samples the current JVM state and returns an array of metric quantities, or {@code null}
   * if the minimum recording delay has not yet elapsed since the last emission.
   *
   * <p>When throttling is active ({@link #minimumRecordingDelayMilliseconds} is non-null and
   * a prior recording exists), the method compares the current time against the last recording
   * timestamp. If the elapsed time is less than the configured minimum delay, {@code null} is
   * returned and no state is updated. Otherwise all fifteen quantities listed in the class
   * Javadoc are sampled and returned, and the last-recording timestamp is updated.
   *
   * @return an array of fifteen {@link Quantity} objects representing the current JVM profile,
   * or {@code null} when the emission is suppressed by the throttle
   */
  @Override
  public Quantity[] record () {

    long now = System.currentTimeMillis();

    if ((minimumRecordingDelayMilliseconds != null) && (lastRecordingTimestamp >= 0) && (minimumRecordingDelayMilliseconds > (now - lastRecordingTimestamp))) {

      return null;
    } else {

      long totalMemorySize;
      long freeMemorySize;
      long heapMemoryMax;

      lastRecordingTimestamp = now;

      return new Quantity[] {
        new Quantity("totalMemorySize", totalMemorySize = JVMState.getTotalMemorySize()),
        new Quantity("freeMemorySize", freeMemorySize = JVMState.getFreeMemorySize()),
        new Quantity("userMemoryPercent", (totalMemorySize - freeMemorySize) * 100.0 / totalMemorySize),
        new Quantity("heapMemoryMax", JVMState.getHeapMemoryMax()),
        new Quantity("heapMemoryUsed", heapMemoryMax = JVMState.getHeapMemoryUsed()),
        new Quantity("processCPUTime", JVMState.getProcessCPUTime()),
        new Quantity("compilationTime", JVMState.getCompilationTime()),
        new Quantity("youngGenerationHeapSize", heapMemoryMax - JVMState.getTenuredMemoryMax()),
        new Quantity("youngCollectionCount", JVMState.getYoungCollectionCount()),
        new Quantity("youngCollectionTime", JVMState.getYoungCollectionTime()),
        new Quantity("oldCollectionCount", JVMState.getOldCollectionCount()),
        new Quantity("oldCollectionTime", JVMState.getOldCollectionTime()),
        new Quantity("edenMemoryUsed", JVMState.getCompilationTime()),
        new Quantity("survivorMemoryUsed", JVMState.getCompilationTime()),
        new Quantity("tenuredMemoryUsed", JVMState.getCompilationTime())
      };
    }
  }
}
