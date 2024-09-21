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

public class InitialState extends PerfState {

  private final long freeMemorySize;
  private final long heapMemoryUsed;
  private final long nanosecondTimestamp;
  private final long processCPUTime;
  private final long youngCollectionCount;
  private final long youngCollectionTime;
  private final long oldCollectionCount;
  private final long oldCollectionTime;
  private final long compilationTime;

  public InitialState () {

    System.gc();

    nanosecondTimestamp = System.nanoTime();
    processCPUTime = getOsFacts().getProcessCpuTime();

    freeMemorySize = getOsFacts().getFreeMemorySize();
    heapMemoryUsed = getCompilationAndHeapFacts().getHeapMemoryUsage().getUsed();

    youngCollectionTime = getGarbageFacts().getYoungCollectionTime();
    youngCollectionCount = getGarbageFacts().getYoungCollectionCount();
    oldCollectionTime = getGarbageFacts().getOldCollectionTime();
    oldCollectionCount = getGarbageFacts().getOldCollectionCount();

    compilationTime = getCompilationAndHeapFacts().getCompilationTime();

    /*
    start.heap = heapMemory.getHeapMemoryUsage();
    start.eden = edenMemoryPool.get();
    start.survivor = survivorMemoryPool.get();
    start.tenured = tenuredMemoryPool.get();

     */
  }

  public long getNanosecondTimestamp () {

    return nanosecondTimestamp;
  }

  public long getProcessCPUTime () {

    return processCPUTime;
  }

  public long getYoungCollectionCount () {

    return youngCollectionCount;
  }

  public long getYoungCollectionTime () {

    return youngCollectionTime;
  }

  public long getOldCollectionCount () {

    return oldCollectionCount;
  }

  public long getOldCollectionTime () {

    return oldCollectionTime;
  }

  public long getCompilationTime () {

    return compilationTime;
  }
}
