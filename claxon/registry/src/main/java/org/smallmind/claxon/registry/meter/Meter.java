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

/**
 * Base contract for all metric meter implementations in the Claxon registry.
 *
 * <p>A {@code Meter} accepts a stream of {@code long} measurements via
 * {@link #update(long)} and, when polled by the registry, produces a snapshot
 * of the accumulated data as an array of named {@link Quantity} values via
 * {@link #record()}. Implementations are responsible for their own internal
 * aggregation strategy (e.g., running totals, moving averages, histograms).</p>
 *
 * <p>Concrete meter types include {@link Gauge}, {@link Histogram},
 * {@link Speedometer}, {@link Tachometer}, {@link Tally}, and {@link Trace}.</p>
 */
public interface Meter {

  /**
   * Incorporates a new measurement into the meter's internal aggregate.
   *
   * <p>The interpretation of {@code value} depends on the concrete implementation.
   * For example, a {@link Tally} treats it as a delta to add to a counter, while a
   * {@link Tachometer} ignores the value entirely and simply counts each call as one event.</p>
   *
   * @param value the measurement to incorporate
   */
  void update (long value);

  /**
   * Captures the meter's current state as an array of named {@link Quantity} values
   * suitable for emission to a monitoring back end.
   *
   * <p>Implementations define both the number and names of the returned quantities.
   * The array must not be {@code null}, but may be empty if the meter has no data to report.</p>
   *
   * @return a non-null array of {@link Quantity} instances representing the current
   * state of this meter
   */
  Quantity[] record ();
}
