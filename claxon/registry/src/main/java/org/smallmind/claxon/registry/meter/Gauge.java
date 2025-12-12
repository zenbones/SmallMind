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
 * Meter that reports minimum, maximum, and average values over the collection interval.
 */
public class Gauge implements Meter {

  private final Bounded bounded;
  private final Averaged averaged;

  /**
   * Creates a gauge with bounded and averaged aggregates.
   */
  public Gauge () {

    bounded = new Bounded();
    averaged = new Averaged();
  }

  /**
   * Updates both min/max and average with the supplied value.
   *
   * @param value value to incorporate
   */
  @Override
  public void update (long value) {

    bounded.update(value);
    averaged.update(value);
  }

  /**
   * Records minimum, maximum, and average quantities for the current window.
   *
   * @return array containing available quantities
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
