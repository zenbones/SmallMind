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
package org.smallmind.claxon.registry;

import java.util.Observable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;
import org.smallmind.nutsnbolts.util.SansResultExecutable;
import org.smallmind.nutsnbolts.util.WithResultExecutable;

public class WorkingInstrumentation implements Instrumentation {

  private final ClaxonRegistry registry;
  private final MeterBuilder<?> builder;
  private final Tag[] tags;
  private final Class<?> caller;
  private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

  public WorkingInstrumentation (ClaxonRegistry registry, Class<?> caller, MeterBuilder<?> builder, Tag... tags) {

    this.registry = registry;
    this.caller = caller;
    this.builder = builder;
    this.tags = tags;
  }

  public WorkingInstrumentation as (TimeUnit timeUnit) {

    this.timeUnit = timeUnit;

    return this;
  }

  public <O extends Observable> O track (O observable) {

    return (observable == null) ? null : registry.track(caller, builder, observable, tags);
  }

  public <T> T track (T measured, Function<T, Long> measurement) {

    return (measurement == null) ? measured : registry.track(caller, builder, measured, measurement, tags);
  }

  public void update (long value) {

    registry.register(caller, builder, tags).update(value);
  }

  public void update (long value, TimeUnit valueTimeUnit) {

    registry.register(caller, builder, tags).update(timeUnit.convert(value, valueTimeUnit));
  }

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
