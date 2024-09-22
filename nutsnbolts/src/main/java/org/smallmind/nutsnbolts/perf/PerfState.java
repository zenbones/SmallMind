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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class PerfState {

  private static final MemoryPools MEMORY_POOLS = new MemoryPools();
  private static final OSFacts OS_FACTS = new OSFacts();
  private static final JVMFacts JVM_FACTS = new JVMFacts();
  private static final GarbageFacts GARBAGE_FACTS = new GarbageFacts();
  private static final CompilationAndHeapFacts COMPILATION_AND_HEAP_FACTS = new CompilationAndHeapFacts();
  private final long millisecondTimestamp;
  private final long totalMemorySize;
  private final long heapMemoryMax;
  private final long tenuredMemoryMax;

  public PerfState () {

    millisecondTimestamp = System.currentTimeMillis();
    totalMemorySize = OS_FACTS.getTotalMemorySize();
    heapMemoryMax = COMPILATION_AND_HEAP_FACTS.getHeapMemoryUsage().getMax();
    tenuredMemoryMax = MEMORY_POOLS.getTenuredMemoryUsage().getMax();
  }

  @XmlTransient
  public MemoryPools getMemoryPools () {

    return MEMORY_POOLS;
  }

  @XmlTransient
  public OSFacts getOsFacts () {

    return OS_FACTS;
  }

  @XmlTransient
  public JVMFacts getJvmFacts () {

    return JVM_FACTS;
  }

  @XmlTransient
  public GarbageFacts getGarbageFacts () {

    return GARBAGE_FACTS;
  }

  @XmlTransient
  public CompilationAndHeapFacts getCompilationAndHeapFacts () {

    return COMPILATION_AND_HEAP_FACTS;
  }

  @XmlTransient
  public String getOsDescription () {

    return OS_FACTS.getDescription();
  }

  @XmlTransient
  public String getJvmDescription () {

    return JVM_FACTS.getDescription();
  }

  @XmlTransient
  public int getCores () {

    return OS_FACTS.getCores();
  }

  @XmlElement
  public long getMillisecondTimestamp () {

    return millisecondTimestamp;
  }

  @XmlElement
  public long getTotalMemorySize () {

    return totalMemorySize;
  }

  @XmlElement
  public long getHeapMemoryMax () {

    return heapMemoryMax;
  }

  @XmlElement
  public long getYoungGenerationHeapSize () {

    return heapMemoryMax - tenuredMemoryMax;
  }
}
