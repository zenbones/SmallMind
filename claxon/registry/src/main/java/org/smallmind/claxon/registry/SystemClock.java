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
package org.smallmind.claxon.registry;

/**
 * {@link Clock} implementation that delegates directly to the JVM system time facilities.
 * Wall time is sourced from {@link System#currentTimeMillis()} and monotonic time from
 * {@link System#nanoTime()}. A single shared instance is available via {@link #instance()};
 * the constructor is not private, allowing subclassing or direct instantiation where needed.
 */
public class SystemClock implements Clock {

  /**
   * Shared singleton instance of the system clock.
   */
  private static final SystemClock SYSTEM_CLOCK = new SystemClock();

  /**
   * Returns the shared singleton instance of {@code SystemClock}.
   *
   * @return the singleton {@link SystemClock}
   */
  public static SystemClock instance () {

    return SYSTEM_CLOCK;
  }

  /**
   * Returns the current wall-clock time in milliseconds since the Unix epoch, as reported
   * by {@link System#currentTimeMillis()}.
   *
   * @return current wall time in milliseconds
   */
  @Override
  public long wallTime () {

    return System.currentTimeMillis();
  }

  /**
   * Returns the current value of the JVM high-resolution monotonic timer in nanoseconds,
   * as reported by {@link System#nanoTime()}. The value is suitable for measuring elapsed
   * time but has no absolute relationship to wall-clock time.
   *
   * @return current monotonic time in nanoseconds
   */
  @Override
  public long monotonicTime () {

    return System.nanoTime();
  }
}
