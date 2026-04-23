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
import org.smallmind.nutsnbolts.time.Stint;

/**
 * Fluent {@link MeterBuilder} implementation for constructing {@link Tachometer} meters.
 *
 * <p>The only configurable parameter is the sliding-window resolution used for count
 * and rate calculations. The default resolution is one second.</p>
 */
public class TachometerBuilder implements MeterBuilder<Tachometer> {

  /**
   * Default sliding-window resolution of one second used when none is explicitly configured.
   */
  private static final Stint ONE_SECOND_STINT = new Stint(1, TimeUnit.SECONDS);

  /**
   * Duration of the sliding window for count and rate calculations; defaults to {@link #ONE_SECOND_STINT}.
   */
  private Stint resolutionStint = ONE_SECOND_STINT;

  /**
   * Sets the sliding-window duration used for count and rate calculations.
   *
   * @param resolutionStint the window duration; must be a positive duration
   * @return this builder, for method chaining
   */
  public MeterBuilder<Tachometer> resolution (Stint resolutionStint) {

    this.resolutionStint = resolutionStint;

    return this;
  }

  /**
   * Builds a {@link Tachometer} meter using the configured sliding-window resolution.
   *
   * @param clock the registry clock forwarded to the {@link Tachometer} for rate tracking
   * @return a fully configured {@link Tachometer} instance
   */
  @Override
  public Tachometer build (Clock clock) {

    return new Tachometer(clock, resolutionStint);
  }
}
