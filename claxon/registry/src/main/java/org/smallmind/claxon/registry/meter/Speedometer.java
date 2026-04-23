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
package org.smallmind.claxon.registry.meter;

import org.smallmind.claxon.registry.Clock;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.QuantityType;
import org.smallmind.claxon.registry.aggregate.Bounded;
import org.smallmind.claxon.registry.aggregate.Paced;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * A {@link Meter} that combines min/max value tracking with event-count and rate
 * measurement over a configurable sliding window.
 *
 * <p>Each call to {@link #update(long)} records the supplied value in a {@link Bounded}
 * aggregate (to track the running minimum and maximum) and increments the event count
 * in a {@link Paced} aggregate by {@code 1}. The value itself is not forwarded to
 * {@link Paced}; only the occurrence of the event is counted.</p>
 *
 * <p>On {@link #record()}, up to four named {@link Quantity} values are returned:
 * {@code "minimum"} and {@code "maximum"} (omitted when no values have been observed),
 * {@code "count"} (typed as {@link QuantityType#COUNT}), and {@code "rate"}.</p>
 */
public class Speedometer implements Meter {

  /**
   * Aggregate that tracks the minimum and maximum values seen since creation.
   */
  private final Bounded bounded;

  /**
   * Aggregate that tracks total event count and per-window rate.
   */
  private final Paced paced;

  /**
   * Creates a new {@code Speedometer} with the given clock and sliding-window duration.
   *
   * @param clock       the clock providing monotonic time for rate calculations
   * @param windowStint the duration of the sliding window over which count and rate
   *                    are computed; must be a positive duration
   */
  public Speedometer (Clock clock, Stint windowStint) {

    bounded = new Bounded();
    paced = new Paced(clock, windowStint);
  }

  /**
   * Records {@code value} in the min/max aggregate and increments the event count by one.
   *
   * <p>Note that the magnitude of {@code value} does not affect the count or rate
   * calculations — only the fact that an event occurred is recorded in the {@link Paced}
   * aggregate.</p>
   *
   * @param value the measurement whose minimum and maximum are tracked; the value itself
   *              is not used for count or rate purposes
   */
  @Override
  public void update (long value) {

    bounded.update(value);
    paced.update(1);
  }

  /**
   * Returns the current minimum, maximum, event count, and event rate as
   * {@link Quantity} instances.
   *
   * <p>The {@code "minimum"} quantity is included only when at least one value has been
   * observed (bounded minimum is less than {@link Long#MAX_VALUE}). The {@code "maximum"}
   * quantity is included only when at least one value has been observed (bounded maximum
   * is greater than {@link Long#MIN_VALUE}). The {@code "count"} and {@code "rate"}
   * quantities are always present.</p>
   *
   * @return an array of between 2 and 4 {@link Quantity} values in the order
   * {@code minimum} (conditional), {@code maximum} (conditional),
   * {@code count}, {@code rate}
   */
  @Override
  public Quantity[] record () {

    Quantity[] quantities;
    double[] measurements = paced.getMeasurements();
    long minimum = bounded.getMinimum();
    long maximum = bounded.getMaximum();
    int size = 2;
    int index = 0;

    if (minimum < Long.MAX_VALUE) {
      size++;
    }
    if (maximum > Long.MIN_VALUE) {
      size++;
    }

    quantities = new Quantity[size];

    if (minimum < Long.MAX_VALUE) {
      quantities[index++] = new Quantity("minimum", minimum);
    }
    if (maximum > Long.MIN_VALUE) {
      quantities[index++] = new Quantity("maximum", maximum);
    }
    quantities[index++] = new Quantity("count", measurements[0], QuantityType.COUNT);
    quantities[index] = new Quantity("rate", measurements[1]);

    return quantities;
  }
}
