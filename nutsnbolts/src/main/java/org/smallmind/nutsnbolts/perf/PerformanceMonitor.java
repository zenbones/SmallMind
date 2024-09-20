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

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMonitor {

/*

  private final AtomicInteger starts = new AtomicInteger();
  private ScheduledFuture<?> memoryPoller;
  private ScheduledExecutorService scheduler;
  private long memoryPollInterval = 250;
  private Stop stop;

  public PerformanceMonitor () {


    // on start
    scheduler = Executors.newSingleThreadScheduledExecutor();
    memoryPoller = scheduler.scheduleWithFixedDelay(this, memoryPollInterval, memoryPollInterval, TimeUnit.MILLISECONDS);

    // --------------------------------------------------------------
  }



  public long getMemoryPollInterval () {

    return memoryPollInterval;
  }

  public void setMemoryPollInterval (long gcPollInterval) {

    this.memoryPollInterval = gcPollInterval;
  }

  public void run () {

    long eden = memoryPools.getEdenMemoryUsage().getUsed();
    long survivor = memoryPools.getSurvivorMemoryUsage().getUsed();
    long tenured = memoryPools.getTenuredMemoryUsage().getUsed();

    if (lastEden < eden)
      stop.edenBytes += eden - lastEden;
    if (lastSurvivor < survivor)
      stop.survivorBytes += survivor - lastSurvivor;
    if (lastTenured <= tenured)
      stop.tenuredBytes += tenured - lastTenured;

    lastEden = eden;
    lastSurvivor = survivor;
    lastTenured = tenured;
  }


  public Start start () {

    synchronized (this) {
      if (starts.incrementAndGet() > 1)
        return null;

      Start start = new Start();
      stop = new Stop();

      System.gc();

      start.heap = heapMemory.getHeapMemoryUsage();
      start.eden = memoryPools.getEdenMemoryUsage();
      start.survivor = memoryPools.getSurvivorMemoryUsage();
      start.tenured = memoryPools.getTenuredMemoryUsage();

      return start;
    }
  }


  public Stop stop () {

    synchronized (this) {
      if (starts.decrementAndGet() > 0)
        return null;


      memoryPoller.cancel(false);
      scheduler.shutdown();

      return stop;
    }
  }

  private static class Base {

    public String EOL = System.lineSeparator();

    public float percent (long dividend, long divisor) {

      if (divisor != 0)
        return (float)dividend * 100 / divisor;
      return Float.NaN;
    }

    public float mebiBytes (long bytes) {

      return (float)bytes / 1024 / 1024;
    }

    public float gibiBytes (long bytes) {

      return (float)bytes / 1024 / 1024 / 1024;
    }
  }

  public static class Start extends Base {



    @Override
    public String toString () {

      StringBuilder builder = new StringBuilder();
      builder.append("========================================").append(EOL);
      builder.append("Monitoring Started at ").append(new Date(date)).append(EOL);
      builder.append("Operative System: ").append(os).append(EOL);
      builder.append("JVM: ").append(jvm).append(EOL);
      builder.append("Processors: ").append(cores).append(EOL);
      builder.append("System Memory: ").append(percent(totalMemory - freeMemory, totalMemory))
        .append("% used of ").append(gibiBytes(totalMemory))
        .append(" GiB").append(EOL);
      builder.append("Used Heap Size: ").append(mebiBytes(heap.getUsed()))
        .append(" MiB").append(EOL);
      builder.append("Max Heap Size: ").append(mebiBytes(heap.getMax()))
        .append(" MiB").append(EOL);
      builder.append("Young Generation Heap Size: ").append(mebiBytes(heap.getMax() - tenured.getMax()))
        .append(" MiB").append(EOL);
      builder.append("- - - - - - - - - - - - - - - - - - - - ");
      return builder.toString();
    }
  }

  public static class Stop extends Base {

    public long edenBytes;
    public long survivorBytes;
    public long tenuredBytes;

    @Override
    public String toString () {

      StringBuilder builder = new StringBuilder();
      builder.append("- - - - - - - - - - - - - - - - - - - - ").append(EOL);
      builder.append("Monitoring Ended at ").append(new Date(date)).append(EOL);
      builder.append("Elapsed Time: ").append(TimeUnit.NANOSECONDS.toMillis(time)).append(" ms").append(EOL);
      builder.append("\tTime in JIT Compilation: ").append(jitTime).append(" ms").append(EOL);
      builder.append("\tTime in Young GC: ").append(youngTime).append(" ms (")
        .append(youngCount).append(" collections)").append(EOL);
      builder.append("\tTime in Old GC: ").append(oldTime).append(" ms (")
        .append(oldCount).append(" collections)").append(EOL);
      builder.append("Garbage Generated in Eden Space: ").append(mebiBytes(edenBytes))
        .append(" MiB").append(EOL);
      builder.append("Garbage Generated in Survivor Space: ").append(mebiBytes(survivorBytes))
        .append(" MiB").append(EOL);
      builder.append("Garbage Generated in Tenured Space: ").append(mebiBytes(tenuredBytes))
        .append(" MiB").append(EOL);
      builder.append("Average CPU Load: ").append(percent(cpuTime, time)).append("/")
        .append(100 * cores).append(EOL);
      builder.append("========================================");
      return builder.toString();
    }
  }
  */
}
