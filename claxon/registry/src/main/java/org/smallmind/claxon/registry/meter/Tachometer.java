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
import org.smallmind.claxon.registry.aggregate.Paced;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * A {@link Meter} that tracks the total number of events and the event rate over a
 * configurable sliding window, without regard to the magnitude of individual values.
 *
 * <p>Each call to {@link #update(long)} is treated as a single event regardless of
 * the supplied {@code value} — the value itself is ignored, and the underlying
 * {@link Paced} aggregate is incremented by {@code 1}. This makes {@code Tachometer}
 * suitable for measuring throughput (e.g., requests per second) where only occurrence
 * matters, not magnitude.</p>
 *
 * <p>On {@link #record()}, two {@link Quantity} values are returned: {@code "count"}
 * (typed as {@link QuantityType#COUNT}) and {@code "rate"}.</p>
 */
public class Tachometer implements Meter {

  /**
   * Aggregate that tracks the total event count and per-window event rate.
   */
  private final Paced paced;

  /**
   * Creates a new {@code Tachometer} with the given clock and sliding-window duration.
   *
   * @param clock       the clock providing monotonic time for rate calculations
   * @param windowStint the duration of the sliding window over which count and rate
   *                    are computed; must be a positive duration
   */
  public Tachometer (Clock clock, Stint windowStint) {

    paced = new Paced(clock, windowStint);
  }

  /**
   * Records one event occurrence in the underlying {@link Paced} aggregate.
   *
   * <p>The supplied {@code value} is completely ignored; each invocation of this
   * method contributes exactly {@code 1} to the event count.</p>
   *
   * @param value ignored; present only to satisfy the {@link Meter} contract
   */
  @Override
  public void update (long value) {

    paced.update(1);
  }

  /**
   * Returns the current event count and event rate as {@link Quantity} instances.
   *
   * <p>The returned array always contains exactly two elements:
   * <ol>
   *   <li>{@code "count"} — total number of events recorded ({@link QuantityType#COUNT})</li>
   *   <li>{@code "rate"} — events per unit time over the configured sliding window</li>
   * </ol>
   *
   * @return an array of exactly two {@link Quantity} values: {@code count} and {@code rate}
   */
  @Override
  public Quantity[] record () {

    double[] measurements = paced.getMeasurements();

    return new Quantity[] {new Quantity("count", measurements[0], QuantityType.COUNT), new Quantity("rate", measurements[1])};
  }
}
