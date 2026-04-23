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
import java.lang.management.OperatingSystemMXBean;

/**
 * Accessor for operating-system level metrics exposed through the JVM's
 * {@link OperatingSystemMXBean}.
 *
 * <p>Static OS information (processor count and description) is captured once at construction
 * time. Dynamic metrics (process CPU time, total memory, and free memory) require the
 * platform-specific {@link com.sun.management.OperatingSystemMXBean} extension; when that
 * interface is not available, {@code -1} is returned as a sentinel indicating that the value
 * cannot be determined.
 */
public class OSFacts {

  /**
   * The underlying OS MXBean obtained from {@link ManagementFactory}; may additionally
   * implement {@link com.sun.management.OperatingSystemMXBean} on Oracle/OpenJDK platforms.
   */
  private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

  /**
   * Human-readable OS description in the form {@code "name version arch"}, captured at
   * construction time.
   */
  private final String description;

  /**
   * The number of processors available to the JVM at construction time, as reported by
   * {@link OperatingSystemMXBean#getAvailableProcessors()}.
   */
  private final int cores;

  /**
   * Captures the available processor count and constructs a human-readable OS description
   * from the OS name, version, and architecture.
   */
  public OSFacts () {

    cores = operatingSystemMXBean.getAvailableProcessors();
    description = String.format("%s %s %s", operatingSystemMXBean.getName(), operatingSystemMXBean.getVersion(), operatingSystemMXBean.getArch());
  }

  /**
   * Returns the number of processors available to the JVM at the time this instance was
   * created.
   *
   * @return the available processor count
   */
  public int getCores () {

    return cores;
  }

  /**
   * Returns a human-readable description of the host operating system in the form
   * {@code "name version arch"}.
   *
   * @return the OS description string
   */
  public String getDescription () {

    return description;
  }

  /**
   * Returns the CPU time used by the JVM process in nanoseconds.
   *
   * <p>This value is only available when the JVM provides the
   * {@link com.sun.management.OperatingSystemMXBean} extension.
   *
   * @return process CPU time in nanoseconds, or {@code -1} if the platform does not support
   * this measurement
   */
  public long getProcessCpuTime () {

    return (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) ? ((com.sun.management.OperatingSystemMXBean)operatingSystemMXBean).getProcessCpuTime() : -1;
  }

  /**
   * Returns the total physical memory size of the host machine.
   *
   * <p>This value is only available when the JVM provides the
   * {@link com.sun.management.OperatingSystemMXBean} extension.
   *
   * @return total physical memory in bytes, or {@code -1} if the platform does not support
   * this measurement
   */
  public long getTotalMemorySize () {

    return (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) ? ((com.sun.management.OperatingSystemMXBean)operatingSystemMXBean).getTotalMemorySize() : -1;
  }

  /**
   * Returns the amount of free physical memory on the host machine.
   *
   * <p>This value is only available when the JVM provides the
   * {@link com.sun.management.OperatingSystemMXBean} extension.
   *
   * @return free physical memory in bytes, or {@code -1} if the platform does not support
   * this measurement
   */
  public long getFreeMemorySize () {

    return (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) ? ((com.sun.management.OperatingSystemMXBean)operatingSystemMXBean).getFreeMemorySize() : -1;
  }
}
