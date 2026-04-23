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

/**
 * Static facade that aggregates JVM and OS runtime metrics from the helper classes
 * {@link MemoryPools}, {@link OSFacts}, {@link GarbageFacts}, and
 * {@link CompilationAndHeapFacts}.
 *
 * <p>All helper instances are created once as class-level constants when this class is first
 * loaded. Static accessor methods delegate directly to those instances, providing a single
 * consistent entry point for all JVM profiling data consumed by {@link ProfileFeature}.
 */
public class JVMState {

  /**
   * Singleton {@link MemoryPools} used to query eden, survivor, and tenured memory usage.
   */
  private static final MemoryPools MEMORY_POOLS = new MemoryPools();

  /**
   * Singleton {@link OSFacts} used to query OS-level metrics such as CPU count and memory
   * sizes.
   */
  private static final OSFacts OS_FACTS = new OSFacts();

  /**
   * Singleton {@link GarbageFacts} used to query young and old GC collection counts and
   * times.
   */
  private static final GarbageFacts GARBAGE_FACTS = new GarbageFacts();

  /**
   * Singleton {@link CompilationAndHeapFacts} used to query JIT compilation time and heap
   * memory usage.
   */
  private static final CompilationAndHeapFacts COMPILATION_AND_HEAP_FACTS = new CompilationAndHeapFacts();

  /**
   * Returns the total physical memory size of the host operating system.
   *
   * @return total physical memory in bytes, or {@code -1} if unsupported on this platform
   */
  public static long getTotalMemorySize () {

    return OS_FACTS.getTotalMemorySize();
  }

  /**
   * Returns the amount of free (unused) physical memory on the host operating system.
   *
   * @return free physical memory in bytes, or {@code -1} if unsupported on this platform
   */
  public static long getFreeMemorySize () {

    return OS_FACTS.getFreeMemorySize();
  }

  /**
   * Returns the maximum amount of heap memory that the JVM will attempt to use.
   *
   * @return maximum heap size in bytes as reported by {@link java.lang.management.MemoryUsage#getMax()}
   */
  public static long getHeapMemoryMax () {

    return COMPILATION_AND_HEAP_FACTS.getHeapMemoryUsage().getMax();
  }

  /**
   * Returns the amount of heap memory currently in use by the JVM.
   *
   * @return used heap memory in bytes as reported by {@link java.lang.management.MemoryUsage#getUsed()}
   */
  public static long getHeapMemoryUsed () {

    return COMPILATION_AND_HEAP_FACTS.getHeapMemoryUsage().getUsed();
  }

  /**
   * Returns the CPU time used by the JVM process.
   *
   * @return process CPU time in nanoseconds, or {@code -1} if unsupported on this platform
   */
  public static long getProcessCPUTime () {

    return OS_FACTS.getProcessCpuTime();
  }

  /**
   * Returns the approximate total elapsed time spent in JIT compilation since JVM startup.
   *
   * @return JIT compilation time in milliseconds
   */
  public static long getCompilationTime () {

    return COMPILATION_AND_HEAP_FACTS.getCompilationTime();
  }

  /**
   * Returns the total number of young-generation garbage collections performed.
   *
   * @return young-generation GC count, or {@code 0} if no young-generation collector was
   * identified
   */
  public static long getYoungCollectionCount () {

    return GARBAGE_FACTS.getYoungCollectionCount();
  }

  /**
   * Returns the total elapsed time spent in young-generation garbage collection.
   *
   * @return young-generation GC time in milliseconds, or {@code 0} if no young-generation
   * collector was identified
   */
  public static long getYoungCollectionTime () {

    return GARBAGE_FACTS.getYoungCollectionTime();
  }

  /**
   * Returns the total number of old-generation garbage collections performed.
   *
   * @return old-generation GC count, or {@code 0} if no old-generation collector was
   * identified
   */
  public static long getOldCollectionCount () {

    return GARBAGE_FACTS.getOldCollectionCount();
  }

  /**
   * Returns the total elapsed time spent in old-generation garbage collection.
   *
   * @return old-generation GC time in milliseconds, or {@code 0} if no old-generation
   * collector was identified
   */
  public static long getOldCollectionTime () {

    return GARBAGE_FACTS.getOldCollectionTime();
  }

  /**
   * Returns the amount of memory currently used in the eden space memory pool.
   *
   * @return eden space used memory in bytes, or {@code 0} if the eden pool is unavailable
   */
  public static long getEdenMemoryUsed () {

    return MEMORY_POOLS.getEdenMemoryUsage().getUsed();
  }

  /**
   * Returns the amount of memory currently used in the survivor space memory pool.
   *
   * @return survivor space used memory in bytes, or {@code 0} if the survivor pool is
   * unavailable
   */
  public static long getSurvivorMemoryUsed () {

    return MEMORY_POOLS.getSurvivorMemoryUsage().getUsed();
  }

  /**
   * Returns the maximum capacity of the tenured (old) generation memory pool.
   *
   * @return tenured space maximum memory in bytes, or {@code 0} if the tenured pool is
   * unavailable
   */
  public static long getTenuredMemoryMax () {

    return MEMORY_POOLS.getTenuredMemoryUsage().getMax();
  }

  /**
   * Returns the amount of memory currently used in the tenured (old) generation memory pool.
   *
   * @return tenured space used memory in bytes, or {@code 0} if the tenured pool is
   * unavailable
   */
  public static long getTenuredMemoryUsed () {

    return MEMORY_POOLS.getTenuredMemoryUsage().getUsed();
  }
}
