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
package org.smallmind.claxon.registry.meter;

import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.Clock;
import org.smallmind.claxon.registry.Percentile;
import org.smallmind.nutsnbolts.time.Stint;

public class HistogramBuilder implements MeterBuilder<Histogram> {

  private static final Stint ONE_SECOND_STINT = new Stint(1, TimeUnit.SECONDS);
  private static final Percentile[] DEFAULT_PERCENTILES = new Percentile[] {new Percentile("p75", 75.0), new Percentile("p95", 95.0), new Percentile("p98", 98.0), new Percentile("p99", 99.0), new Percentile("p999", 99.9)};
  // order is important here, leave the construction as the last static
  private static final HistogramBuilder DEFAULT_BUILDER = new HistogramBuilder();

  private final Stint resolutionStint = ONE_SECOND_STINT;
  private Percentile[] percentiles = DEFAULT_PERCENTILES;
  private long lowestDiscernibleValue = 1;
  private long highestTrackableValue = 3600000L;
  private int numberOfSignificantValueDigits = 2;

  public static HistogramBuilder instance () {

    return DEFAULT_BUILDER;
  }

  public MeterBuilder<Histogram> lowestDiscernibleValue (long lowestDiscernibleValue) {

    this.lowestDiscernibleValue = lowestDiscernibleValue;

    return this;
  }

  public MeterBuilder<Histogram> highestTrackableValue (long highestTrackableValue) {

    this.highestTrackableValue = highestTrackableValue;

    return this;
  }

  public MeterBuilder<Histogram> numberOfSignificantValueDigits (int numberOfSignificantValueDigits) {

    this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;

    return this;
  }

  public MeterBuilder<Histogram> percentiles (Percentile... percentiles) {

    this.percentiles = percentiles;

    return this;
  }

  public MeterBuilder<Histogram> resolution (Stint resolutionStint) {

   // this.resolutionStint = resolutionStint;

    return this;
  }

  @Override
  public Histogram build (Clock clock) {

    return new Histogram(clock, lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, resolutionStint, percentiles);
  }
}
