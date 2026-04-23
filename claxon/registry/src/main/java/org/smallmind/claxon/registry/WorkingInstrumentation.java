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
 * Active {@link Instrumentation} implementation that registers meters with a
 * {@link ClaxonRegistry} and records measurement updates or execution timings. Unlike
 * {@link UnpluggedInstrumentation}, every method in this class interacts with the registry
 * to ensure that metrics are captured and forwarded to the configured emitters.
 *
 * <p>The time unit used when reporting elapsed durations defaults to
 * {@link TimeUnit#MILLISECONDS} and can be changed via {@link #as(TimeUnit)}.
 */
public class WorkingInstrumentation implements Instrumentation {

  /**
   * Registry used to look up or create meters for this instrumentation.
   */
  private final ClaxonRegistry registry;

  /**
   * Builder that describes the type and configuration of the meter to register.
   */
  private final MeterBuilder<? extends Meter> builder;

  /**
   * Tags attached to every meter created by this instrumentation.
   */
  private final Tag[] tags;

  /**
   * The class on whose behalf metrics are being recorded.
   */
  private final Class<?> caller;

  /**
   * Time unit applied when converting nanosecond durations before updating a meter.
   * Defaults to {@link TimeUnit#MILLISECONDS}.
   */
  private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

  /**
   * Constructs a working instrumentation bound to the given registry, caller, meter builder,
   * and tags.
   *
   * @param registry registry used to register and retrieve meters
   * @param caller   the class on whose behalf metrics are recorded; used as part of the meter key
   * @param builder  builder that describes how to construct the meter if it does not yet exist
   * @param tags     dimensional tags attached to the meter; may be empty but must not be {@code null}
   */
  public WorkingInstrumentation (ClaxonRegistry registry, Class<?> caller, MeterBuilder<? extends Meter> builder, Tag... tags) {

    this.registry = registry;
    this.caller = caller;
    this.builder = builder;
    this.tags = tags;
  }

  /**
   * Returns a new view of this instrumentation that reports elapsed durations in the
   * specified time unit. The change affects all subsequent calls to {@link #update(long)},
   * {@link #on(SansResultExecutable)}, and {@link #on(WithResultExecutable)}.
   *
   * @param timeUnit the desired time unit for elapsed-duration reporting
   * @return this instrumentation instance with the updated time unit
   */
  @Override
  public WorkingInstrumentation as (TimeUnit timeUnit) {

    this.timeUnit = timeUnit;

    return this;
  }

  /**
   * Registers a meter for the measured object and arranges for the registry to periodically
   * sample it using the supplied measurement function. If {@code measurement} is {@code null},
   * the measured object is returned as-is without any tracking being established.
   *
   * @param measured    the object whose state should be periodically measured
   * @param measurement a function that extracts a {@code long} sample value from the object;
   *                    {@code null} disables tracking
   * @param <T>         the type of the measured object
   * @return the {@code measured} object, unmodified
   */
  @Override
  public <T> T track (T measured, Function<T, Long> measurement) {

    return (measurement == null) ? measured : registry.track(caller, builder, measured, measurement, tags);
  }

  /**
   * Records a raw value directly to the meter registered for this instrumentation. The value
   * is passed to the meter without any time-unit conversion.
   *
   * @param value the raw value to record
   */
  @Override
  public void update (long value) {

    registry.register(caller, builder, tags).update(value);
  }

  /**
   * Records a value expressed in the given time unit, converting it to the time unit
   * configured on this instrumentation before passing it to the meter.
   *
   * @param value         the value to record
   * @param valueTimeUnit the time unit in which {@code value} is expressed
   */
  @Override
  public void update (long value, TimeUnit valueTimeUnit) {

    registry.register(caller, builder, tags).update(timeUnit.convert(value, valueTimeUnit));
  }

  /**
   * Executes the supplied code and records the elapsed duration to the meter. The duration
   * is measured using the registry's configured {@link Clock} and converted to the
   * instrumentation's time unit before being recorded.
   *
   * @param sansResultExecutable the executable to time; must not be {@code null}
   * @throws Throwable any exception propagated from {@link SansResultExecutable#execute()}
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
   * Executes the supplied code, records the elapsed duration to the meter, and returns the
   * result. The duration is measured using the registry's configured {@link Clock} and
   * converted to the instrumentation's time unit before being recorded. If the executable is
   * {@code null}, {@code null} is returned immediately without registering or updating any meter.
   *
   * @param withResultExecutable the executable to time; may be {@code null}
   * @param <T>                  the return type of the executable
   * @return the value returned by the executable, or {@code null} if the executable is {@code null}
   * @throws Throwable any exception propagated from {@link WithResultExecutable#execute()}
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
