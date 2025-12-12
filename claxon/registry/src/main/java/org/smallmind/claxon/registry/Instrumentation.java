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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.smallmind.nutsnbolts.util.SansResultExecutable;
import org.smallmind.nutsnbolts.util.WithResultExecutable;

/**
 * Strategy interface for objects that can be instrumented with metrics.
 * Implementations wrap application code and report timing or counts to the backing registry.
 */
public interface Instrumentation {

  /**
   * Returns a view of this instrumentation that reports using the supplied time unit.
   *
   * @param timeUnit the desired {@link TimeUnit} for emitted durations
   * @return a new or adjusted instrumentation using the requested unit
   */
  Instrumentation as (TimeUnit timeUnit);

  /**
   * Tracks a measured value derived from a domain object.
   *
   * @param measured    the domain object to measure
   * @param measurement a function that extracts the measured value from the domain object
   * @param <T>         the type of the measured object
   * @return the same domain object for fluent usage
   */
  <T> T track (T measured, Function<T, Long> measurement);

  /**
   * Records a raw value using the instrumentation's default time unit.
   *
   * @param value the value to record
   */
  void update (long value);

  /**
   * Records a raw value with an explicit time unit.
   *
   * @param value         the value to record
   * @param valueTimeUnit the {@link TimeUnit} associated with the value
   */
  void update (long value, TimeUnit valueTimeUnit);

  /**
   * Executes code while automatically recording execution time or other metric.
   *
   * @param sansResultExecutable executable that returns no result
   * @throws Throwable propagated from the executable
   */
  void on (SansResultExecutable sansResultExecutable)
    throws Throwable;

  /**
   * Executes code while automatically recording execution time or other metric.
   *
   * @param withResultExecutable executable that returns a result
   * @param <T>                  the result type
   * @return the result of the executable
   * @throws Throwable propagated from the executable
   */
  <T> T on (WithResultExecutable<T> withResultExecutable)
    throws Throwable;
}
