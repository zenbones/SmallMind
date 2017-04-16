/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.instrument;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.Thread.State;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class VirtualMachineInfo {

  private static final VirtualMachineInfo INSTANCE = new VirtualMachineInfo(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans(), ManagementFactory.getOperatingSystemMXBean(), ManagementFactory.getThreadMXBean(), ManagementFactory.getGarbageCollectorMXBeans(), ManagementFactory.getRuntimeMXBean(), ManagementFactory.getPlatformMBeanServer());
  private static final int MAX_STACK_TRACE_DEPTH = 100;

  public static VirtualMachineInfo getInstance () {

    return INSTANCE;
  }

  private final MBeanServer mBeanServer;
  private final MemoryMXBean memory;
  private final OperatingSystemMXBean os;
  private final ThreadMXBean threads;
  private final List<MemoryPoolMXBean> memoryPools;
  private final List<GarbageCollectorMXBean> garbageCollectors;
  private final RuntimeMXBean runtime;

  VirtualMachineInfo (MemoryMXBean memory, List<MemoryPoolMXBean> memoryPools, OperatingSystemMXBean os, ThreadMXBean threads, List<GarbageCollectorMXBean> garbageCollectors, RuntimeMXBean runtime, MBeanServer mBeanServer) {

    this.memory = memory;
    this.memoryPools = memoryPools;
    this.os = os;
    this.threads = threads;
    this.garbageCollectors = garbageCollectors;
    this.runtime = runtime;
    this.mBeanServer = mBeanServer;
  }

  public String getVersion () {

    return System.getProperty("java.runtime.version");
  }

  public String getName () {

    return System.getProperty("java.vm.name");
  }

  public long getUptime () {

    return TimeUnit.MILLISECONDS.toSeconds(runtime.getUptime());
  }

  public int getThreadCount () {

    return threads.getThreadCount();
  }

  public int getDaemonThreadCount () {

    return threads.getDaemonThreadCount();
  }

  public double getTotalInit () {

    return memory.getHeapMemoryUsage().getInit() +
      memory.getNonHeapMemoryUsage().getInit();
  }

  public double getTotalUsed () {

    return memory.getHeapMemoryUsage().getUsed() +
      memory.getNonHeapMemoryUsage().getUsed();
  }

  public double getTotalMax () {

    return memory.getHeapMemoryUsage().getMax() +
      memory.getNonHeapMemoryUsage().getMax();
  }

  public double getTotalCommitted () {

    return memory.getHeapMemoryUsage().getCommitted() +
      memory.getNonHeapMemoryUsage().getCommitted();
  }

  public double getHeapInit () {

    return memory.getHeapMemoryUsage().getInit();
  }

  public double getHeapUsed () {

    return memory.getHeapMemoryUsage().getUsed();
  }

  public double getHeapMax () {

    return memory.getHeapMemoryUsage().getMax();
  }

  public double getHeapCommitted () {

    return memory.getHeapMemoryUsage().getCommitted();
  }

  public double getHeapUsage () {

    final MemoryUsage usage = memory.getHeapMemoryUsage();
    return usage.getUsed() / (double)usage.getMax();
  }

  public double getNonHeapUsage () {

    final MemoryUsage usage = memory.getNonHeapMemoryUsage();
    return usage.getUsed() / (double)usage.getMax();
  }

  public Map<String, Double> getMemoryPoolUsage () {

    final Map<String, Double> pools = new TreeMap<String, Double>();
    for (MemoryPoolMXBean pool : memoryPools) {
      final double max = pool.getUsage().getMax() == -1 ?
        pool.getUsage().getCommitted() :
        pool.getUsage().getMax();
      pools.put(pool.getName(), pool.getUsage().getUsed() / max);
    }
    return Collections.unmodifiableMap(pools);
  }

  public double getFileDescriptorUsage () {

    try {
      final Method getOpenFileDescriptorCount = os.getClass().getDeclaredMethod("getOpenFileDescriptorCount");
      getOpenFileDescriptorCount.setAccessible(true);
      final Long openFds = (Long)getOpenFileDescriptorCount.invoke(os);
      final Method getMaxFileDescriptorCount = os.getClass().getDeclaredMethod("getMaxFileDescriptorCount");
      getMaxFileDescriptorCount.setAccessible(true);
      final Long maxFds = (Long)getMaxFileDescriptorCount.invoke(os);
      return openFds.doubleValue() / maxFds.doubleValue();
    }
    catch (NoSuchMethodException e) {
      return Double.NaN;
    }
    catch (IllegalAccessException e) {
      return Double.NaN;
    }
    catch (InvocationTargetException e) {
      return Double.NaN;
    }
  }

  public Map<String, GarbageCollectorStats> getGarbageCollectors () {

    final Map<String, GarbageCollectorStats> stats = new HashMap<String, GarbageCollectorStats>();
    for (GarbageCollectorMXBean gc : garbageCollectors) {
      stats.put(gc.getName(),
        new GarbageCollectorStats(gc.getCollectionCount(),
          gc.getCollectionTime()));
    }
    return Collections.unmodifiableMap(stats);
  }

  public Set<String> getDeadlockedThreads () {

    final long[] threadIds = threads.findDeadlockedThreads();
    if (threadIds != null) {
      final Set<String> threads = new HashSet<String>();
      for (ThreadInfo info : this.threads.getThreadInfo(threadIds, MAX_STACK_TRACE_DEPTH)) {
        final StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : info.getStackTrace()) {
          stackTrace.append("\t at ").append(element.toString()).append('\n');
        }

        threads.add(
          String.format(
            "%s locked on %s (owned by %s):\n%s",
            info.getThreadName(), info.getLockName(),
            info.getLockOwnerName(),
            stackTrace.toString()
          )
        );
      }
      return Collections.unmodifiableSet(threads);
    }
    return Collections.emptySet();
  }

  public Map<State, Double> getThreadStatePercentages () {

    final Map<State, Double> conditions = new HashMap<State, Double>();
    for (State state : State.values()) {
      conditions.put(state, 0.0);
    }

    final long[] allThreadIds = threads.getAllThreadIds();
    final ThreadInfo[] allThreads = threads.getThreadInfo(allThreadIds);
    int liveCount = 0;
    for (ThreadInfo info : allThreads) {
      if (info != null) {
        final State state = info.getThreadState();
        conditions.put(state, conditions.get(state) + 1);
        liveCount++;
      }
    }
    for (State state : new ArrayList<State>(conditions.keySet())) {
      conditions.put(state, conditions.get(state) / liveCount);
    }

    return Collections.unmodifiableMap(conditions);
  }

  public void getThreadDump (OutputStream out) {

    final ThreadInfo[] threads = this.threads.dumpAllThreads(true, true);
    final PrintWriter writer = new PrintWriter(out, true);

    for (int ti = threads.length - 1; ti >= 0; ti--) {
      final ThreadInfo t = threads[ti];
      writer.printf("%s id=%d state=%s",
        t.getThreadName(),
        t.getThreadId(),
        t.getThreadState());
      final LockInfo lock = t.getLockInfo();
      if (lock != null && t.getThreadState() != Thread.State.BLOCKED) {
        writer.printf("\n    - waiting on <0x%08x> (a %s)",
          lock.getIdentityHashCode(),
          lock.getClassName());
        writer.printf("\n    - locked <0x%08x> (a %s)",
          lock.getIdentityHashCode(),
          lock.getClassName());
      }
      else if (lock != null && t.getThreadState() == Thread.State.BLOCKED) {
        writer.printf("\n    - waiting to lock <0x%08x> (a %s)",
          lock.getIdentityHashCode(),
          lock.getClassName());
      }

      if (t.isSuspended()) {
        writer.print(" (suspended)");
      }

      if (t.isInNative()) {
        writer.print(" (running in native)");
      }

      writer.println();
      if (t.getLockOwnerName() != null) {
        writer.printf("     owned by %s id=%d\n", t.getLockOwnerName(), t.getLockOwnerId());
      }

      final StackTraceElement[] elements = t.getStackTrace();
      final MonitorInfo[] monitors = t.getLockedMonitors();

      for (int i = 0; i < elements.length; i++) {
        final StackTraceElement element = elements[i];
        writer.printf("    at %s\n", element);
        for (int j = 1; j < monitors.length; j++) {
          final MonitorInfo monitor = monitors[j];
          if (monitor.getLockedStackDepth() == i) {
            writer.printf("      - locked %s\n", monitor);
          }
        }
      }
      writer.println();

      final LockInfo[] locks = t.getLockedSynchronizers();
      if (locks.length > 0) {
        writer.printf("    Locked synchronizers: count = %d\n", locks.length);
        for (LockInfo l : locks) {
          writer.printf("      - %s\n", l);
        }
        writer.println();
      }
    }

    writer.println();
    writer.flush();
  }

  public Map<String, BufferPoolStats> getBufferPoolStats () {

    try {
      final String[] attributes = {"Count", "MemoryUsed", "TotalCapacity"};

      final ObjectName direct = new ObjectName("java.nio:type=BufferPool,name=direct");
      final ObjectName mapped = new ObjectName("java.nio:type=BufferPool,name=mapped");

      final AttributeList directAttributes = mBeanServer.getAttributes(direct, attributes);
      final AttributeList mappedAttributes = mBeanServer.getAttributes(mapped, attributes);

      final Map<String, BufferPoolStats> stats = new TreeMap<String, BufferPoolStats>();

      final BufferPoolStats directStats = new BufferPoolStats((Long)((Attribute)directAttributes.get(0)).getValue(),
        (Long)((Attribute)directAttributes.get(1)).getValue(),
        (Long)((Attribute)directAttributes.get(2)).getValue());

      stats.put("direct", directStats);

      final BufferPoolStats mappedStats = new BufferPoolStats((Long)((Attribute)mappedAttributes.get(0)).getValue(),
        (Long)((Attribute)mappedAttributes.get(1)).getValue(),
        (Long)((Attribute)mappedAttributes.get(2)).getValue());

      stats.put("mapped", mappedStats);

      return Collections.unmodifiableMap(stats);
    }
    catch (JMException e) {

      return Collections.emptyMap();
    }
  }

  // Per-GC statistics.
  public static class GarbageCollectorStats {

    private final long runs, timeMS;

    private GarbageCollectorStats (long runs, long timeMS) {

      this.runs = runs;
      this.timeMS = timeMS;
    }

    public long getRuns () {

      return runs;
    }

    public long getTime (TimeUnit unit) {

      return unit.convert(timeMS, TimeUnit.MILLISECONDS);
    }
  }

  public static class BufferPoolStats {

    private final long count, memoryUsed, totalCapacity;

    private BufferPoolStats (long count, long memoryUsed, long totalCapacity) {

      this.count = count;
      this.memoryUsed = memoryUsed;
      this.totalCapacity = totalCapacity;
    }

    public long getCount () {

      return count;
    }

    public long getMemoryUsed () {

      return memoryUsed;
    }

    public long getTotalCapacity () {

      return totalCapacity;
    }
  }
}
