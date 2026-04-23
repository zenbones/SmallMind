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

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Thin wrapper around the JVM's {@link CompilationMXBean} and {@link MemoryMXBean} that
 * provides convenient access to JIT compilation time and heap memory usage for use by
 * profiling features.
 *
 * <p>Both MXBean references are obtained once at construction time from
 * {@link ManagementFactory} and are reused for all subsequent calls.
 */
public class CompilationAndHeapFacts {

  /**
   * The JVM compilation MXBean used to retrieve the accumulated JIT compilation time.
   */
  private final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();

  /**
   * The JVM memory MXBean used to retrieve heap and non-heap memory usage statistics.
   */
  private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

  /**
   * Returns the approximate total elapsed time spent in JIT compilation since the JVM started.
   *
   * @return total JIT compilation time in milliseconds, as reported by
   * {@link CompilationMXBean#getTotalCompilationTime()}
   */
  public long getCompilationTime () {

    return compilationMXBean.getTotalCompilationTime();
  }

  /**
   * Returns a snapshot of the current heap memory usage including initial, used, committed,
   * and maximum values.
   *
   * @return the current {@link MemoryUsage} for the heap memory pool
   */
  public MemoryUsage getHeapMemoryUsage () {

    return memoryMXBean.getHeapMemoryUsage();
  }
}
