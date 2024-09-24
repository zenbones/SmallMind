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
package org.smallmind.claxon.exotic.jvm;

public class JVMState {

  private static final MemoryPools MEMORY_POOLS = new MemoryPools();
  private static final OSFacts OS_FACTS = new OSFacts();
  private static final GarbageFacts GARBAGE_FACTS = new GarbageFacts();
  private static final CompilationAndHeapFacts COMPILATION_AND_HEAP_FACTS = new CompilationAndHeapFacts();

  public static long getTotalMemorySize () {

    return OS_FACTS.getTotalMemorySize();
  }

  public static long getFreeMemorySize () {

    return OS_FACTS.getFreeMemorySize();
  }

  public static long getHeapMemoryMax () {

    return COMPILATION_AND_HEAP_FACTS.getHeapMemoryUsage().getMax();
  }

  public static long getHeapMemoryUsed () {

    return COMPILATION_AND_HEAP_FACTS.getHeapMemoryUsage().getUsed();
  }

  public static long getProcessCPUTime () {

    return OS_FACTS.getProcessCpuTime();
  }

  public static long getCompilationTime () {

    return COMPILATION_AND_HEAP_FACTS.getCompilationTime();
  }

  public static long getYoungCollectionCount () {

    return GARBAGE_FACTS.getYoungCollectionCount();
  }

  public static long getYoungCollectionTime () {

    return GARBAGE_FACTS.getYoungCollectionTime();
  }

  public static long getOldCollectionCount () {

    return GARBAGE_FACTS.getOldCollectionCount();
  }

  public static long getOldCollectionTime () {

    return GARBAGE_FACTS.getOldCollectionTime();
  }

  public static long getEdenMemoryUsed () {

    return MEMORY_POOLS.getEdenMemoryUsage().getUsed();
  }

  public static long getSurvivorMemoryUsed () {

    return MEMORY_POOLS.getSurvivorMemoryUsage().getUsed();
  }

  public static long getTenuredMemoryMax () {

    return MEMORY_POOLS.getTenuredMemoryUsage().getMax();
  }

  public static long getTenuredMemoryUsed () {

    return MEMORY_POOLS.getTenuredMemoryUsage().getUsed();
  }
}
