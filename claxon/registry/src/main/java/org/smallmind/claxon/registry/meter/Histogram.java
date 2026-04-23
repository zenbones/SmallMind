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
import org.smallmind.claxon.registry.Percentile;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.QuantityType;
import org.smallmind.claxon.registry.aggregate.HistogramTime;
import org.smallmind.claxon.registry.aggregate.Stratified;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * A {@link Meter} backed by an HdrHistogram that reports count, rate, minimum,
 * maximum, mean, and an optional set of configurable percentiles over a rolling
 * collection window.
 *
 * <p>Values are accumulated in a {@link Stratified} aggregate that manages
 * time-windowed HdrHistogram intervals. On each call to {@link #record()}, the
 * current interval snapshot is consumed and converted into a fixed set of base
 * quantities ({@code "count"}, {@code "rate"}, {@code "minimum"}, {@code "maximum"},
 * {@code "mean"}) followed by one quantity per configured {@link Percentile}.</p>
 */
public class Histogram implements Meter {

  /**
   * Time-windowed HdrHistogram aggregate used to collect and snapshot recorded values.
   */
  private final Stratified stratified;

  /**
   * Percentile definitions whose values are appended to the base quantities on each
   * {@link #record()} call; may be {@code null} or empty if no percentiles are desired.
   */
  private final Percentile[] percentiles;

  /**
   * Creates a new {@code Histogram} meter with the specified HdrHistogram parameters
   * and resolution window.
   *
   * @param clock                          clock supplying monotonic time for the rolling window
   * @param lowestDiscernibleValue         the smallest value the histogram is able to distinguish;
   *                                       must be a positive integer
   * @param highestTrackableValue          the largest value the histogram is configured to track
   *                                       without overflow
   * @param numberOfSignificantValueDigits the number of significant decimal digits of precision
   *                                       maintained by the histogram (1–5)
   * @param resolutionStint                the duration of each rolling histogram interval; controls
   *                                       the granularity of the rate calculation
   * @param percentiles                    zero or more {@link Percentile} definitions specifying
   *                                       which percentile values to include in each recording;
   *                                       may be omitted entirely
   */
  public Histogram (Clock clock, long lowestDiscernibleValue, long highestTrackableValue, int numberOfSignificantValueDigits, Stint resolutionStint, Percentile... percentiles) {

    this.percentiles = percentiles;

    stratified = new Stratified(clock, lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, resolutionStint);
  }

  /**
   * Records {@code value} in the underlying HdrHistogram.
   *
   * @param value the non-negative value to record
   */
  @Override
  public void update (long value) {

    stratified.update(value);
  }

  /**
   * Produces a snapshot of the current histogram interval and converts it into
   * named {@link Quantity} instances.
   *
   * <p>The returned array always contains the following quantities in order:
   * <ol>
   *   <li>{@code "count"} — total number of recorded values ({@link QuantityType#COUNT})</li>
   *   <li>{@code "rate"} — count scaled by the interval time factor to express events per second</li>
   *   <li>{@code "minimum"} — lowest recorded value in the interval</li>
   *   <li>{@code "maximum"} — highest recorded value in the interval</li>
   *   <li>{@code "mean"} — arithmetic mean of all recorded values in the interval</li>
   * </ol>
   * followed by one entry for each {@link Percentile} configured at construction time,
   * using the percentile's name and the value at that percentile boundary.</p>
   *
   * @return an array of {@link Quantity} values representing the histogram snapshot;
   * length equals 5 plus the number of configured percentiles
   */
  @Override
  public Quantity[] record () {

    HistogramTime snapshot = stratified.get();
    Quantity[] basicQuantities = new Quantity[] {
      new Quantity("count", snapshot.getHistogram().getTotalCount(), QuantityType.COUNT),
      new Quantity("rate", snapshot.getHistogram().getTotalCount() * snapshot.getTimeFactor()),
      new Quantity("minimum", snapshot.getHistogram().getMinValue()),
      new Quantity("maximum", snapshot.getHistogram().getMaxValue()),
      new Quantity("mean", snapshot.getHistogram().getMean())};
    Quantity[] allQuantities = new Quantity[basicQuantities.length + ((percentiles == null) ? 0 : percentiles.length)];
    int index = 0;

    System.arraycopy(basicQuantities, 0, allQuantities, 0, basicQuantities.length);
    if (percentiles != null) {
      for (Percentile percentile : percentiles) {
        allQuantities[basicQuantities.length + (index++)] = new Quantity(percentile.getName(), snapshot.getHistogram().getValueAtPercentile(percentile.getValue()));
      }
    }

    return allQuantities;
  }
}
