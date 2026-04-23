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

import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.aggregate.Averaged;
import org.smallmind.claxon.registry.aggregate.Bounded;

/**
 * A {@link Meter} that reports the minimum, maximum, and average of all values
 * submitted during the collection interval.
 *
 * <p>Each call to {@link #update(long)} feeds both a {@link Bounded} aggregate
 * (tracking minimum and maximum) and an {@link Averaged} aggregate (tracking the
 * running mean). On {@link #record()}, up to three named {@link Quantity} values
 * are emitted: {@code "minimum"}, {@code "maximum"}, and {@code "average"}.
 * The minimum and maximum quantities are omitted when no values have been recorded
 * (i.e., when the bounded aggregate still holds its sentinel initial values).</p>
 */
public class Gauge implements Meter {

  /**
   * Aggregate that tracks the minimum and maximum values seen since creation.
   */
  private final Bounded bounded;

  /**
   * Aggregate that computes a running arithmetic mean of all submitted values.
   */
  private final Averaged averaged;

  /**
   * Creates a new {@code Gauge} with freshly initialised {@link Bounded} and
   * {@link Averaged} aggregates.
   */
  public Gauge () {

    bounded = new Bounded();
    averaged = new Averaged();
  }

  /**
   * Incorporates {@code value} into both the min/max and average aggregates.
   *
   * @param value the measurement to record
   */
  @Override
  public void update (long value) {

    bounded.update(value);
    averaged.update(value);
  }

  /**
   * Captures the current minimum, maximum, and average measurements as
   * {@link Quantity} instances.
   *
   * <p>The {@code "minimum"} quantity is included only when the bounded aggregate
   * has received at least one update (i.e., its minimum is less than
   * {@link Long#MAX_VALUE}). The {@code "maximum"} quantity is included only when
   * the bounded aggregate has received at least one update (i.e., its maximum is
   * greater than {@link Long#MIN_VALUE}). The {@code "average"} quantity is always
   * present.</p>
   *
   * @return an array containing the available {@link Quantity} values for this
   * collection window; the array length varies between 1 and 3 depending
   * on whether minimum and maximum have been observed
   */
  @Override
  public Quantity[] record () {

    Quantity[] quantities;
    long minimum = bounded.getMinimum();
    long maximum = bounded.getMaximum();
    double average = averaged.getAverage();
    int size = 1;
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
    quantities[index] = new Quantity("average", average);

    return quantities;
  }
}
