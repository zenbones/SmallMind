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

public class Speedometer implements Meter {

  private final Bounded bounded;
  private final Paced paced;

  public Speedometer (Clock clock, Stint windowStint) {

    bounded = new Bounded();
    paced = new Paced(clock, windowStint);
  }

  @Override
  public void update (long value) {

    bounded.update(value);
    paced.update(1);
  }

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
