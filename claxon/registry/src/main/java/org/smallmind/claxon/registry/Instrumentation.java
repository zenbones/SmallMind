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
 * Fluent interface that wraps a backing {@link org.smallmind.claxon.registry.meter.Meter}
 * and provides convenient methods for recording measurements, timing executable blocks,
 * and tracking weakly referenced domain objects.
 *
 * <p>Implementations must be safe to use from any thread. Two concrete implementations
 * exist:
 * <ul>
 *   <li>{@code WorkingInstrumentation} — routes all calls to an active
 *       {@link ClaxonRegistry}.</li>
 *   <li>{@code UnpluggedInstrumentation} — silently discards all calls when no
 *       registry is available.</li>
 * </ul>
 *
 * <p>Instances are obtained via {@link Instrument#with(Class, org.smallmind.claxon.registry.meter.MeterBuilder, Tag...)}.
 */
public interface Instrumentation {

  /**
   * Returns a view of this instrumentation that reports duration values in the given
   * {@link TimeUnit}.
   *
   * @param timeUnit the time unit to use when recording elapsed durations
   * @return an instrumentation instance configured to report in the requested unit
   */
  Instrumentation as (TimeUnit timeUnit);

  /**
   * Begins tracking a weakly referenced domain object, recording a fresh measurement
   * derived from it on each collection interval until the object is garbage-collected.
   *
   * @param measured    the domain object to track
   * @param measurement a function that extracts a {@code long} measurement from
   *                    {@code measured} (e.g., a queue depth or buffer size)
   * @param <T>         the type of the domain object
   * @return {@code measured}, unchanged, to support fluent assignment patterns
   */
  <T> T track (T measured, Function<T, Long> measurement);

  /**
   * Records a single raw {@code long} value using this instrumentation's default
   * time unit.
   *
   * @param value the value to record
   */
  void update (long value);

  /**
   * Records a single raw {@code long} value, converting it from {@code valueTimeUnit}
   * into the instrumentation's default time unit before storage.
   *
   * @param value         the value to record
   * @param valueTimeUnit the {@link TimeUnit} in which {@code value} is expressed
   */
  void update (long value, TimeUnit valueTimeUnit);

  /**
   * Executes {@code sansResultExecutable}, recording elapsed time (or incrementing a
   * counter, depending on the backing meter type) around the call.
   *
   * @param sansResultExecutable the code block to execute and measure
   * @throws Throwable any exception or error thrown by {@code sansResultExecutable}
   */
  void on (SansResultExecutable sansResultExecutable)
    throws Throwable;

  /**
   * Executes {@code withResultExecutable}, recording elapsed time (or incrementing a
   * counter, depending on the backing meter type) around the call, and returns its result.
   *
   * @param withResultExecutable the code block to execute and measure
   * @param <T>                  the type of the value returned by the executable
   * @return the value returned by {@code withResultExecutable}
   * @throws Throwable any exception or error thrown by {@code withResultExecutable}
   */
  <T> T on (WithResultExecutable<T> withResultExecutable)
    throws Throwable;
}
