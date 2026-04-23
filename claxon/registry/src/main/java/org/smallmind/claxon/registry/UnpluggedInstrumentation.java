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
 * No-operation {@link Instrumentation} implementation that executes any supplied work
 * without recording metrics. This is the instrumentation equivalent of a null object: all
 * measurement methods are safe no-ops, all delegate methods pass through to the wrapped
 * executable, and no meters are registered with any registry. It is typically used when
 * instrumentation is disabled or when a registry has not yet been configured.
 */
public class UnpluggedInstrumentation implements Instrumentation {

  /**
   * Returns this instance unchanged; no time-unit conversion is meaningful for a no-op
   * implementation.
   *
   * @param timeUnit ignored
   * @return this {@code UnpluggedInstrumentation} instance
   */
  @Override
  public Instrumentation as (TimeUnit timeUnit) {

    return this;
  }

  /**
   * Returns the measured object without registering a meter or attaching any tracking
   * callback.
   *
   * @param measured    the object to be measured
   * @param measurement a function that would ordinarily extract a long value from the object;
   *                    ignored by this implementation
   * @param <T>         the type of the measured object
   * @return the {@code measured} object, unmodified
   */
  @Override
  public <T> T track (T measured, Function<T, Long> measurement) {

    return measured;
  }

  /**
   * Accepts a measurement value and discards it without recording.
   *
   * @param value the value that would ordinarily be recorded; ignored
   */
  @Override
  public void update (long value) {

  }

  /**
   * Accepts a measurement value and its time unit and discards both without recording.
   *
   * @param value         the value that would ordinarily be recorded; ignored
   * @param valueTimeUnit the time unit of the value; ignored
   */
  @Override
  public void update (long value, TimeUnit valueTimeUnit) {

  }

  /**
   * Executes the supplied {@link SansResultExecutable} without recording any timing
   * information.
   *
   * @param sansResultExecutable the executable to run
   * @throws Throwable any exception propagated from {@link SansResultExecutable#execute()}
   */
  @Override
  public void on (SansResultExecutable sansResultExecutable)
    throws Throwable {

    sansResultExecutable.execute();
  }

  /**
   * Executes the supplied {@link WithResultExecutable} without recording any timing
   * information and returns its result.
   *
   * @param withResultExecutable the executable to run
   * @param <T>                  the return type of the executable
   * @return the value returned by {@link WithResultExecutable#execute()}
   * @throws Throwable any exception propagated from {@link WithResultExecutable#execute()}
   */
  @Override
  public <T> T on (WithResultExecutable<T> withResultExecutable)
    throws Throwable {

    return withResultExecutable.execute();
  }
}
