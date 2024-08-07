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
package org.smallmind.claxon.registry.aggregate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.smallmind.claxon.registry.Clock;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.time.StintUtility;

public class Paced implements Aggregate {

  private final Clock clock;
  private final LongAdder adder = new LongAdder();
  private final double nanosecondsInWindow;
  private long markTime;

  public Paced (Clock clock) {

    this(clock, new Stint(1, TimeUnit.SECONDS));
  }

  public Paced (Clock clock, Stint windowStint) {

    this.clock = clock;

    nanosecondsInWindow = StintUtility.convertToDouble(windowStint.getTime(), windowStint.getTimeUnit(), TimeUnit.NANOSECONDS);
    markTime = clock.monotonicTime();
  }

  public void inc () {

    add(1);
  }

  public void add (long delta) {

    if (delta < 0) {
      throw new IllegalArgumentException(delta + " is less than 0");
    } else {
      adder.add(delta);
    }
  }

  @Override
  public void update (long value) {

    add(value);
  }

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
