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
package org.smallmind.claxon.registry.aggregate;

import org.HdrHistogram.Histogram;

/**
 * Pairs an HdrHistogram snapshot with the time-normalization factor needed to project its
 * counts onto the configured collection window.
 *
 * <p>Because {@link Stratified} flips recorders on demand rather than on a fixed schedule,
 * the actual elapsed time between two consecutive reads may differ from the nominal window
 * duration. The {@link #timeFactor} encodes the ratio
 * {@code actualElapsed / nominalWindowNanos}, allowing consumers to scale counts and rates
 * so they are comparable across reporting intervals of varying length.</p>
 */
public class HistogramTime {

  /**
   * The HdrHistogram snapshot captured at the end of a collection window.
   */
  private final Histogram histogram;

  /**
   * Ratio of actual elapsed nanoseconds to the configured window duration.
   * A value of {@code 1.0} means the collection interval matched the window exactly;
   * values above or below {@code 1.0} indicate a longer or shorter actual interval.
   */
  private final double timeFactor;

  /**
   * Constructs a timed histogram wrapper.
   *
   * @param histogram  the HdrHistogram snapshot for the completed collection window; must not be {@code null}
   * @param timeFactor ratio of actual elapsed time to the nominal window duration, used to
   *                   normalise per-window counts and rates
   */
  public HistogramTime (Histogram histogram, double timeFactor) {

    this.histogram = histogram;
    this.timeFactor = timeFactor;
  }

  /**
   * Returns the HdrHistogram snapshot captured at the end of the collection window.
   *
   * @return the histogram snapshot; never {@code null}
   */
  public Histogram getHistogram () {

    return histogram;
  }

  /**
   * Returns the scaling factor that normalises histogram counts to the configured window duration.
   *
   * <p>Consumers should divide raw counts by this factor to obtain rates expressed per nominal
   * window, or multiply per-window targets by this factor to obtain raw thresholds.</p>
   *
   * @return ratio of actual elapsed nanoseconds to the nominal window nanoseconds
   */
  public double getTimeFactor () {

    return timeFactor;
  }
}
