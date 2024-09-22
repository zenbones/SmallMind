/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.perf;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "final")
public class FinalState extends PerfState {

  private long freeMemorySize;
  private long heapMemoryUsed;
  private long elapsedClockNanoseconds;
  private long totalProcessCPUTime;
  private long totalYoungCollectionCount;
  private long totalYoungCollectionTime;
  private long totalOldCollectionCount;
  private long totalOldCollectionTime;
  private long totalCompilationTime;
  private long lastEdenMemoryUsed;
  private long lastSurvivorMemoryUsed;
  private long lastTenuredMemoryUsed;
  private long edenBytesConsumed;
  private long survivorBytesConsumed;
  private long tenuredBytesConsumed;

  public FinalState () {

    lastEdenMemoryUsed = getMemoryPools().getEdenMemoryUsage().getUsed();
    lastSurvivorMemoryUsed = getMemoryPools().getSurvivorMemoryUsage().getUsed();
    lastTenuredMemoryUsed = getMemoryPools().getTenuredMemoryUsage().getUsed();
  }

  public void update () {

    long currentEdenMemoryUsed = getMemoryPools().getEdenMemoryUsage().getUsed();
    long currentSurvivorMemoryUsed = getMemoryPools().getSurvivorMemoryUsage().getUsed();
    long currentTenuredMemoryUsed = getMemoryPools().getTenuredMemoryUsage().getUsed();

    if (lastEdenMemoryUsed < currentEdenMemoryUsed) {
      edenBytesConsumed += currentEdenMemoryUsed - this.lastEdenMemoryUsed;
    }
    if (this.lastSurvivorMemoryUsed < currentSurvivorMemoryUsed) {
      survivorBytesConsumed += currentSurvivorMemoryUsed - this.lastSurvivorMemoryUsed;
    }
    if (this.lastTenuredMemoryUsed <= currentTenuredMemoryUsed) {
      tenuredBytesConsumed += currentTenuredMemoryUsed - this.lastTenuredMemoryUsed;
    }

    this.lastEdenMemoryUsed = currentEdenMemoryUsed;
    this.lastSurvivorMemoryUsed = currentSurvivorMemoryUsed;
    this.lastTenuredMemoryUsed = currentTenuredMemoryUsed;
  }

  public void stop (InitialState initialState) {

    long initialProcessCPUTime;

    elapsedClockNanoseconds = System.nanoTime() - initialState.getNanosecondTimestamp();
    totalProcessCPUTime = ((initialProcessCPUTime = initialState.getProcessCPUTime()) < 0) ? -1 : getOsFacts().getProcessCpuTime() - initialProcessCPUTime;

    totalCompilationTime = getCompilationAndHeapFacts().getCompilationTime() - initialState.getCompilationTime();

    freeMemorySize = getOsFacts().getFreeMemorySize();
    heapMemoryUsed = getCompilationAndHeapFacts().getHeapMemoryUsage().getUsed();

    totalYoungCollectionTime = getGarbageFacts().getYoungCollectionTime() - initialState.getYoungCollectionTime();
    totalYoungCollectionCount = getGarbageFacts().getYoungCollectionCount() - initialState.getYoungCollectionCount();
    totalOldCollectionTime = getGarbageFacts().getOldCollectionTime() - initialState.getOldCollectionTime();
    totalOldCollectionCount = getGarbageFacts().getOldCollectionCount() - initialState.getOldCollectionCount();
  }

  @XmlElement
  public long getFreeMemorySize () {

    return freeMemorySize;
  }

  @XmlElement
  public long getHeapMemoryUsed () {

    return heapMemoryUsed;
  }

  @XmlElement
  public long getElapsedClockNanoseconds () {

    return elapsedClockNanoseconds;
  }

  @XmlElement
  public long getTotalProcessCPUTime () {

    return totalProcessCPUTime;
  }

  @XmlElement
  public long getTotalYoungCollectionCount () {

    return totalYoungCollectionCount;
  }

  @XmlElement
  public long getTotalYoungCollectionTime () {

    return totalYoungCollectionTime;
  }

  @XmlElement
  public long getTotalOldCollectionCount () {

    return totalOldCollectionCount;
  }

  @XmlElement
  public long getTotalOldCollectionTime () {

    return totalOldCollectionTime;
  }

  @XmlElement
  public long getTotalCompilationTime () {

    return totalCompilationTime;
  }

  @XmlElement
  public long getEdenBytesConsumed () {

    return edenBytesConsumed;
  }

  @XmlElement
  public long getSurvivorBytesConsumed () {

    return survivorBytesConsumed;
  }

  @XmlElement
  public long getTenuredBytesConsumed () {

    return tenuredBytesConsumed;
  }

  @XmlElement
  public double getUsedMemoryPercent () {

    return (getTotalMemorySize() - freeMemorySize) * 100.0 / getTotalMemorySize();
  }

  @XmlElement
  public double getCPUUtilizationPercent () {

    return totalProcessCPUTime * 100.0 / elapsedClockNanoseconds;
  }
}
