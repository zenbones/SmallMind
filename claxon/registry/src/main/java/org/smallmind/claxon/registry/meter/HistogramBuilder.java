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
import org.smallmind.claxon.registry.Percentile;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * Fluent {@link MeterBuilder} implementation for constructing {@link Histogram} meters.
 *
 * <p>All setter methods return {@code this} (typed as {@link MeterBuilder}{@code <Histogram>})
 * to support method chaining. Default values are:</p>
 * <ul>
 *   <li>{@code lowestDiscernibleValue} — {@code 1}</li>
 *   <li>{@code highestTrackableValue} — {@code 3_600_000} (1 hour in milliseconds)</li>
 *   <li>{@code numberOfSignificantValueDigits} — {@code 2}</li>
 *   <li>{@code resolutionStint} — 1 second</li>
 *   <li>{@code percentiles} — p75, p95, p98, p99, p99.9</li>
 * </ul>
 */
public class HistogramBuilder implements MeterBuilder<Histogram> {

  /**
   * Default resolution window of one second used when none is explicitly configured.
   */
  private static final Stint ONE_SECOND_STINT = new Stint(1, TimeUnit.SECONDS);

  /**
   * Default set of percentiles emitted by a histogram when none are explicitly configured:
   * 75th, 95th, 98th, 99th, and 99.9th percentiles.
   */
  private static final Percentile[] DEFAULT_PERCENTILES = new Percentile[] {new Percentile("p75", 75.0), new Percentile("p95", 95.0), new Percentile("p98", 98.0), new Percentile("p99", 99.0), new Percentile("p999", 99.9)};

  /**
   * Duration of each rolling histogram interval; defaults to {@link #ONE_SECOND_STINT}.
   */
  private Stint resolutionStint = ONE_SECOND_STINT;

  /**
   * Percentiles to include in each recording; defaults to {@link #DEFAULT_PERCENTILES}.
   */
  private Percentile[] percentiles = DEFAULT_PERCENTILES;

  /**
   * Smallest value the histogram is able to distinguish; defaults to {@code 1}.
   */
  private long lowestDiscernibleValue = 1;

  /**
   * Largest value the histogram is configured to track without overflow; defaults to {@code 3_600_000}.
   */
  private long highestTrackableValue = 3600000L;

  /**
   * Number of significant decimal digits of precision maintained by the histogram; defaults to {@code 2}.
   */
  private int numberOfSignificantValueDigits = 2;

  /**
   * Sets the smallest value the histogram should track.
   *
   * @param lowestDiscernibleValue the minimum distinguishable value; must be a positive integer
   * @return this builder, for method chaining
   */
  public MeterBuilder<Histogram> lowestDiscernibleValue (long lowestDiscernibleValue) {

    this.lowestDiscernibleValue = lowestDiscernibleValue;

    return this;
  }

  /**
   * Sets the largest value the histogram should track without overflow.
   *
   * @param highestTrackableValue the maximum trackable value; must be at least twice the
   *                              {@code lowestDiscernibleValue}
   * @return this builder, for method chaining
   */
  public MeterBuilder<Histogram> highestTrackableValue (long highestTrackableValue) {

    this.highestTrackableValue = highestTrackableValue;

    return this;
  }

  /**
   * Sets the number of significant decimal digits of precision maintained by the histogram.
   *
   * @param numberOfSignificantValueDigits precision level; valid range is 1 through 5
   * @return this builder, for method chaining
   */
  public MeterBuilder<Histogram> numberOfSignificantValueDigits (int numberOfSignificantValueDigits) {

    this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;

    return this;
  }

  /**
   * Configures the set of percentiles to be emitted alongside the base quantities on each
   * {@link Histogram#record()} call.
   *
   * @param percentiles one or more {@link Percentile} definitions; passing an empty array
   *                    suppresses all percentile quantities
   * @return this builder, for method chaining
   */
  public MeterBuilder<Histogram> percentiles (Percentile... percentiles) {

    this.percentiles = percentiles;

    return this;
  }

  /**
   * Sets the duration of each rolling histogram interval, which also governs the
   * time factor applied to rate calculations.
   *
   * @param resolutionStint the interval duration; must be a positive duration
   * @return this builder, for method chaining
   */
  public MeterBuilder<Histogram> resolution (Stint resolutionStint) {

    this.resolutionStint = resolutionStint;

    return this;
  }

  /**
   * Builds a {@link Histogram} meter using the parameters accumulated on this builder.
   *
   * @param clock the clock provided by the registry, used for rolling-window management
   * @return a fully configured {@link Histogram} instance
   */
  @Override
  public Histogram build (Clock clock) {

    return new Histogram(clock, lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, resolutionStint, percentiles);
  }
}
