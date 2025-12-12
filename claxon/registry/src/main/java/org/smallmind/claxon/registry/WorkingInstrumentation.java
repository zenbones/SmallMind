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
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;
import org.smallmind.nutsnbolts.util.SansResultExecutable;
import org.smallmind.nutsnbolts.util.WithResultExecutable;

/**
 * Active instrumentation that registers meters with the registry and records updates or timings.
 */
public class WorkingInstrumentation implements Instrumentation {

  private final ClaxonRegistry registry;
  private final MeterBuilder<? extends Meter> builder;
  private final Tag[] tags;
  private final Class<?> caller;
  private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

  /**
   * Creates an instrumentation tied to a registry, caller, meter builder, and tags.
   *
   * @param registry registry used to register meters
   * @param caller   class requesting metrics
   * @param builder  builder that constructs meters
   * @param tags     tags to attach to the meter
   */
  public WorkingInstrumentation (ClaxonRegistry registry, Class<?> caller, MeterBuilder<? extends Meter> builder, Tag... tags) {

    this.registry = registry;
    this.caller = caller;
    this.builder = builder;
    this.tags = tags;
  }

  /**
   * Uses a custom time unit for subsequent updates.
   *
   * @param timeUnit desired time unit for updates
   * @return this instrumentation for chaining
   */
  @Override
  public WorkingInstrumentation as (TimeUnit timeUnit) {

    this.timeUnit = timeUnit;

    return this;
  }

  /**
   * Registers a meter for the measured object and schedules updates via the supplied measurement function.
   *
   * @param measured    object to measure
   * @param measurement function converting the object into a long value
   * @param <T>         measured type
   * @return the measured object
   */
  @Override
  public <T> T track (T measured, Function<T, Long> measurement) {

    return (measurement == null) ? measured : registry.track(caller, builder, measured, measurement, tags);
  }

  /**
   * Records a value using the current time unit.
   *
   * @param value value to record
   */
  @Override
  public void update (long value) {

    registry.register(caller, builder, tags).update(value);
  }

  /**
   * Records a value while converting from the provided time unit to the configured one.
   *
   * @param value         value to record
   * @param valueTimeUnit time unit associated with the value
   */
  @Override
  public void update (long value, TimeUnit valueTimeUnit) {

    registry.register(caller, builder, tags).update(timeUnit.convert(value, valueTimeUnit));
  }

  /**
   * Executes code while recording the execution duration.
   *
   * @param sansResultExecutable executable to run
   * @throws Throwable propagated from the executable
   */
  @Override
  public void on (SansResultExecutable sansResultExecutable)
    throws Throwable {

    if (sansResultExecutable != null) {

      Meter meter = registry.register(caller, builder, tags);
      Clock clock = registry.getConfiguration().getClock();
      long start = clock.monotonicTime();

      sansResultExecutable.execute();
      meter.update(timeUnit.convert(clock.monotonicTime() - start, TimeUnit.NANOSECONDS));
    }
  }

  /**
   * Executes code while recording the execution duration and returning the result.
   *
   * @param withResultExecutable executable to run
   * @param <T>                  result type
   * @return the executable result or {@code null} when the executable is absent
   * @throws Throwable propagated from the executable
   */
  @Override
  public <T> T on (WithResultExecutable<T> withResultExecutable)
    throws Throwable {

    if (withResultExecutable == null) {

      return null;
    } else {

      T result;
      Meter meter = registry.register(caller, builder, tags);
      Clock clock = registry.getConfiguration().getClock();
      long start = clock.monotonicTime();

      result = withResultExecutable.execute();
      meter.update(timeUnit.convert(clock.monotonicTime() - start, TimeUnit.NANOSECONDS));

      return result;
    }
  }
}
