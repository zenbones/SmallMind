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
package org.smallmind.claxon.registry.aggregate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.smallmind.claxon.registry.Clock;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.time.StintUtility;

/**
 * Thread-safe {@link Aggregate} that accumulates a running count and derives a time-normalised
 * rate from it each time {@link #getMeasurements()} is called.
 *
 * <p>Counts are maintained by a {@link LongAdder} for high-throughput, contention-free
 * increments. On each call to {@link #getMeasurements()} the elapsed nanoseconds since the
 * previous reading are measured and used to project the raw count onto the configured window,
 * yielding a rate expressed in units of {@code count per window}.</p>
 *
 * <p>Negative deltas are rejected by {@link #add(long)} to prevent accidental counter
 * corruption; use {@link #update(long)} only with non-negative values.</p>
 */
public class Paced implements Aggregate {

  /**
   * Source of monotonic timestamps used to compute elapsed time between {@link #getMeasurements()} calls.
   */
  private final Clock clock;

  /**
   * Lock-free counter accumulating all increments since the last {@link #getMeasurements()} call.
   */
  private final LongAdder adder = new LongAdder();

  /**
   * The configured window duration expressed in nanoseconds, used to scale raw counts into rates.
   */
  private final double nanosecondsInWindow;

  /**
   * Monotonic timestamp (nanoseconds) of the most recent {@link #getMeasurements()} call.
   */
  private long markTime;

  /**
   * Constructs a paced aggregate with a one-second reporting window.
   *
   * @param clock source of monotonic time used to compute elapsed intervals
   */
  public Paced (Clock clock) {

    this(clock, new Stint(1, TimeUnit.SECONDS));
  }

  /**
   * Constructs a paced aggregate with a custom reporting window.
   *
   * @param clock       source of monotonic time used to compute elapsed intervals
   * @param windowStint duration of the normalisation window; must be positive
   */
  public Paced (Clock clock, Stint windowStint) {

    this.clock = clock;

    nanosecondsInWindow = StintUtility.convertToDouble(windowStint.getTime(), windowStint.getTimeUnit(), TimeUnit.NANOSECONDS);
    markTime = clock.monotonicTime();
  }

  /**
   * Increments the counter by one.
   */
  public void inc () {

    add(1);
  }

  /**
   * Adds a non-negative delta to the running counter.
   *
   * @param delta the amount to add; must be {@code >= 0}
   * @throws IllegalArgumentException if {@code delta} is negative
   */
  public void add (long delta) {

    if (delta < 0) {
      throw new IllegalArgumentException(delta + " is less than 0");
    } else {
      adder.add(delta);
    }
  }

  /**
   * Adds the supplied value to the running counter via {@link #add(long)}.
   *
   * @param value the measurement to add; must be {@code >= 0}
   * @throws IllegalArgumentException if {@code value} is negative
   */
  @Override
  public void update (long value) {

    add(value);
  }

  /**
   * Returns the count and time-normalised rate accumulated since the last call, then resets
   * the counter and advances the time mark.
   *
   * <p>The returned array always has exactly two elements:</p>
   * <ol>
   *   <li>index 0 — raw count of events recorded since the previous call</li>
   *   <li>index 1 — rate expressed in events per configured window duration</li>
   * </ol>
   *
   * @return a two-element {@code double[]} containing {@code [count, rate]}
   */
  public synchronized double[] getMeasurements () {

    double rate;
    double timeFactor;
    long now = clock.monotonicTime();
    long count = adder.sum();

    timeFactor = nanosecondsInWindow / (now - markTime);
    rate = count * timeFactor;

    adder.add(-count);
    markTime = now;

    return new double[] {count, rate};
  }
}
