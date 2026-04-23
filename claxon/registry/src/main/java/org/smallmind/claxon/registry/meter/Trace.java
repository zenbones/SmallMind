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

import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.Clock;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Window;
import org.smallmind.claxon.registry.aggregate.Pursued;

/**
 * A {@link Meter} that reports multiple exponentially weighted moving averages (EWMAs),
 * one for each configured {@link Window}.
 *
 * <p>Values submitted via {@link #update(long)} are forwarded to a {@link Pursued}
 * aggregate, which maintains a separate EWMA for every window size. On each call to
 * {@link #record()}, one {@link Quantity} per window is returned, named after the window
 * and carrying the current moving-average value for that window's decay constant.</p>
 *
 * <p>This meter is well suited to smoothing noisy signals over different time horizons
 * simultaneously — for example, reporting 1-minute, 5-minute, and 15-minute load averages
 * in the style of Unix {@code uptime}.</p>
 */
public class Trace implements Meter {

  /**
   * Aggregate that maintains one EWMA per configured window.
   */
  private final Pursued pursued;

  /**
   * The window definitions provided at construction time; used to map the ordered
   * EWMA values returned by {@link Pursued#getMovingAverages()} to named quantities.
   */
  private final Window[] windows;

  /**
   * Creates a new {@code Trace} meter with one EWMA per supplied {@link Window}.
   *
   * @param clock          the clock providing monotonic time for the EWMA decay calculations
   * @param windowTimeUnit the {@link TimeUnit} applied to the numeric value of each
   *                       {@link Window}; for example, {@link TimeUnit#MINUTES} interprets
   *                       a window value of {@code 5} as a 5-minute window
   * @param windows        one or more {@link Window} definitions specifying the name and
   *                       numeric size of each moving-average window; must not be empty
   */
  public Trace (Clock clock, TimeUnit windowTimeUnit, Window... windows) {

    long[] windowTimes = new long[windows.length];
    int index = 0;

    this.windows = windows;

    for (Window window : windows) {
      windowTimes[index++] = window.getValue();
    }

    pursued = new Pursued(clock, windowTimeUnit, windowTimes);
  }

  /**
   * Incorporates {@code value} into all configured EWMAs.
   *
   * @param value the measurement to include in the moving averages
   */
  @Override
  public void update (long value) {

    pursued.update(value);
  }

  /**
   * Returns the current exponentially weighted moving average for each configured window
   * as a named {@link Quantity}.
   *
   * <p>The returned array has the same length and ordering as the {@link Window} array
   * supplied at construction. Each quantity is named after its corresponding window
   * (e.g., {@code "m1"}, {@code "m5"}, {@code "m15"}) and carries the EWMA value
   * computed over that window's decay period.</p>
   *
   * @return an array of {@link Quantity} values, one per configured window, in
   * construction order
   */
  @Override
  public Quantity[] record () {

    Quantity[] quantities = new Quantity[windows.length];
    double[] values = pursued.getMovingAverages();
    int index = 0;

    for (Window window : windows) {
      quantities[index] = new Quantity(window.getName(), values[index++]);
    }

    return quantities;
  }
}
