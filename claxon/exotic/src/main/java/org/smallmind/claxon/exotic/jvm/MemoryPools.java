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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.function.Supplier;

/**
 * Discovers and caches references to the JVM's eden, survivor, and tenured (old) memory pool
 * MXBeans, and provides live usage snapshots for each.
 *
 * <p>Pool MXBeans are identified by name at construction time. When a pool cannot be
 * identified (e.g. because a non-generational GC such as ZGC or Shenandoah is in use),
 * the corresponding supplier returns a zeroed {@link MemoryUsage} sentinel rather than
 * throwing.
 *
 * <p>Recognised pool names:
 * <ul>
 *   <li>Eden: PS Eden Space, Par Eden Space, G1 Eden Space</li>
 *   <li>Survivor: PS Survivor Space, Par Survivor Space, G1 Survivor Space</li>
 *   <li>Tenured / old: PS Old Gen, CMS Old Gen, G1 Old Gen, Shenandoah, ZHeap</li>
 * </ul>
 */
public class MemoryPools {

  /**
   * A zeroed {@link MemoryUsage} instance returned when a memory pool is unavailable on the
   * current JVM.
   */
  private static final MemoryUsage ZERO_MEMORY_USAGE = new MemoryUsage(0, 0, 0, 0);

  /**
   * Supplier that returns the current usage of the eden memory pool, or
   * {@link #ZERO_MEMORY_USAGE} when the pool is unavailable.
   */
  private final Supplier<MemoryUsage> edenMemoryPool;

  /**
   * Supplier that returns the current usage of the survivor memory pool, or
   * {@link #ZERO_MEMORY_USAGE} when the pool is unavailable.
   */
  private final Supplier<MemoryUsage> survivorMemoryPool;

  /**
   * Supplier that returns the current usage of the tenured (old) memory pool, or
   * {@link #ZERO_MEMORY_USAGE} when the pool is unavailable.
   */
  private final Supplier<MemoryUsage> tenuredMemoryPool;

  /**
   * Discovers memory pool MXBeans from {@link ManagementFactory} and wires usage suppliers
   * for the eden, survivor, and tenured pools. Unrecognised or missing pools fall back to a
   * supplier that returns {@link #ZERO_MEMORY_USAGE}.
   */
  public MemoryPools () {

    List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
    MemoryPoolMXBean edenMemoryPoolMXBean = null;
    MemoryPoolMXBean survivorMemoryPoolMXBean = null;
    MemoryPoolMXBean oldMempryPoolMXBean = null;

    for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
      switch (memoryPoolMXBean.getName()) {
        case "PS Eden Space", "Par Eden Space", "G1 Eden Space" -> edenMemoryPoolMXBean = memoryPoolMXBean;
        case "PS Survivor Space", "Par Survivor Space", "G1 Survivor Space" -> survivorMemoryPoolMXBean = memoryPoolMXBean;
        case "PS Old Gen", "CMS Old Gen", "G1 Old Gen", "Shenandoah", "ZHeap" -> oldMempryPoolMXBean = memoryPoolMXBean;
      }
    }

    edenMemoryPool = edenMemoryPoolMXBean == null ? () -> ZERO_MEMORY_USAGE : edenMemoryPoolMXBean::getUsage;
    survivorMemoryPool = survivorMemoryPoolMXBean == null ? () -> ZERO_MEMORY_USAGE : survivorMemoryPoolMXBean::getUsage;
    tenuredMemoryPool = oldMempryPoolMXBean == null ? () -> ZERO_MEMORY_USAGE : oldMempryPoolMXBean::getUsage;
  }

  /**
   * Returns the current memory usage of the eden space pool.
   *
   * @return a live {@link MemoryUsage} snapshot, or a zeroed instance when the eden pool is
   * unavailable on this JVM
   */
  public MemoryUsage getEdenMemoryUsage () {

    return edenMemoryPool.get();
  }

  /**
   * Returns the current memory usage of the survivor space pool.
   *
   * @return a live {@link MemoryUsage} snapshot, or a zeroed instance when the survivor pool
   * is unavailable on this JVM
   */
  public MemoryUsage getSurvivorMemoryUsage () {

    return survivorMemoryPool.get();
  }

  /**
   * Returns the current memory usage of the tenured (old) generation pool.
   *
   * @return a live {@link MemoryUsage} snapshot, or a zeroed instance when the tenured pool
   * is unavailable on this JVM
   */
  public MemoryUsage getTenuredMemoryUsage () {

    return tenuredMemoryPool.get();
  }
}
