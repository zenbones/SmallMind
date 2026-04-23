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
package org.smallmind.claxon.registry.json;

import org.smallmind.claxon.registry.Percentile;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;

/**
 * JSON-mapped configuration properties for a histogram meter. Instances of this class are
 * populated by the JSON doppelganger framework and subsequently consumed by
 * {@link HistogramParser} to build a configured histogram. All fields are optional; a
 * {@code null} value indicates that the corresponding histogram builder default should be
 * used.
 */
@Doppelganger(namespace = "http://org.smallmind/claxon/registry")
public class HistogramProperties {

  /**
   * Resolution window controlling how frequently histogram data is rotated or snapshotted.
   */
  @View(adapter = StintXmlAdapter.class, idioms = @Idiom(visibility = IN))
  private Stint resolutionStint;

  /**
   * Percentile definitions that should be emitted alongside raw histogram statistics.
   */
  @View(idioms = @Idiom(visibility = IN))
  private Percentile[] percentiles;

  /**
   * The lowest value that can be discerned from a neighboring value by the histogram.
   * Maps directly to the HdrHistogram {@code lowestDiscernibleValue} parameter.
   */
  @View(idioms = @Idiom(visibility = IN))
  private Long lowestDiscernibleValue;

  /**
   * The highest value that the histogram is able to track without overflow.
   * Maps directly to the HdrHistogram {@code highestTrackableValue} parameter.
   */
  @View(idioms = @Idiom(visibility = IN))
  private Long highestTrackableValue;

  /**
   * The number of significant decimal digits to which the histogram will maintain value
   * resolution and separation. Maps directly to the HdrHistogram
   * {@code numberOfSignificantValueDigits} parameter.
   */
  @View(idioms = @Idiom(visibility = IN))
  private Integer numberOfSignificantValueDigits;

  /**
   * Returns the resolution stint that governs the histogram's rotation interval.
   *
   * @return the configured {@link Stint}, or {@code null} if not set
   */
  public Stint getResolutionStint () {

    return resolutionStint;
  }

  /**
   * Sets the resolution stint that governs the histogram's rotation interval.
   *
   * @param resolutionStint the time window to apply, or {@code null} to use the builder default
   */
  public void setResolutionStint (Stint resolutionStint) {

    this.resolutionStint = resolutionStint;
  }

  /**
   * Returns the percentile definitions that should be emitted by the histogram.
   *
   * @return array of {@link Percentile} objects, or {@code null} if not set
   */
  public Percentile[] getPercentiles () {

    return percentiles;
  }

  /**
   * Sets the percentile definitions that should be emitted by the histogram.
   *
   * @param percentiles array of percentile definitions, or {@code null} to use builder defaults
   */
  public void setPercentiles (Percentile[] percentiles) {

    this.percentiles = percentiles;
  }

  /**
   * Returns the lowest value the histogram can distinguish from a neighboring value.
   *
   * @return the lowest discernible value, or {@code null} if not set
   */
  public Long getLowestDiscernibleValue () {

    return lowestDiscernibleValue;
  }

  /**
   * Sets the lowest value the histogram can distinguish from a neighboring value.
   *
   * @param lowestDiscernibleValue the minimum discernible value, or {@code null} to use the builder default
   */
  public void setLowestDiscernibleValue (Long lowestDiscernibleValue) {

    this.lowestDiscernibleValue = lowestDiscernibleValue;
  }

  /**
   * Returns the highest value the histogram is able to track without overflow.
   *
   * @return the highest trackable value, or {@code null} if not set
   */
  public Long getHighestTrackableValue () {

    return highestTrackableValue;
  }

  /**
   * Sets the highest value the histogram is able to track without overflow.
   *
   * @param highestTrackableValue the maximum trackable value, or {@code null} to use the builder default
   */
  public void setHighestTrackableValue (Long highestTrackableValue) {

    this.highestTrackableValue = highestTrackableValue;
  }

  /**
   * Returns the number of significant decimal digits maintained by the histogram.
   *
   * @return the digit precision, or {@code null} if not set
   */
  public Integer getNumberOfSignificantValueDigits () {

    return numberOfSignificantValueDigits;
  }

  /**
   * Sets the number of significant decimal digits maintained by the histogram.
   *
   * @param numberOfSignificantValueDigits the digit precision, or {@code null} to use the builder default
   */
  public void setNumberOfSignificantValueDigits (Integer numberOfSignificantValueDigits) {

    this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
  }
}
