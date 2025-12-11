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

public class ProfileFeature implements Feature {

  private final Tag[] tags;
  private final String name;
  private final Long minimumRecordingDelayMilliseconds;
  private long lastRecordingTimestamp = -1;

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

  @Override
  public String getName () {

    return name;
  }

  @Override
  public Tag[] getTags () {

    return tags;
  }

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
