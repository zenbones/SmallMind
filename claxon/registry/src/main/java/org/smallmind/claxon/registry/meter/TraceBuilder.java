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
import org.smallmind.claxon.registry.Window;

/**
 * Fluent {@link MeterBuilder} implementation for constructing {@link Trace} meters.
 *
 * <p>Two parameters can be configured:</p>
 * <ul>
 *   <li>{@code windowTimeUnit} — the {@link TimeUnit} applied to the numeric size of
 *       each {@link Window}; defaults to {@link TimeUnit#MINUTES}.</li>
 *   <li>{@code windows} — the set of named EWMA windows to track; defaults to
 *       {@code m1} (1 minute), {@code m5} (5 minutes), and {@code m15} (15 minutes).</li>
 * </ul>
 */
public class TraceBuilder implements MeterBuilder<Trace> {

  /**
   * Default set of windows emitted by a trace meter when none are explicitly configured:
   * 1-minute ({@code "m1"}), 5-minute ({@code "m5"}), and 15-minute ({@code "m15"}) EWMAs.
   */
  private static final Window[] DEFAULT_WINDOWS = new Window[] {new Window("m1", 1), new Window("m5", 5), new Window("m15", 15)};

  /**
   * The time unit applied to each window's numeric value; defaults to {@link TimeUnit#MINUTES}.
   */
  private TimeUnit windowTimeUnit = TimeUnit.MINUTES;

  /**
   * The window definitions used to create per-window EWMAs; defaults to {@link #DEFAULT_WINDOWS}.
   */
  private Window[] windows = DEFAULT_WINDOWS;

  /**
   * Sets the {@link TimeUnit} applied to the numeric size of each configured {@link Window}.
   *
   * @param windowTimeUnit the time unit for all window values; must not be {@code null}
   * @return this builder, for method chaining
   */
  public MeterBuilder<Trace> windowTimeUnit (TimeUnit windowTimeUnit) {

    this.windowTimeUnit = windowTimeUnit;

    return this;
  }

  /**
   * Sets the window definitions that determine the EWMA decay constants tracked by the meter.
   *
   * @param windows one or more {@link Window} instances specifying a name and numeric size
   *                for each moving-average window; must not be {@code null} or empty
   * @return this builder, for method chaining
   */
  public MeterBuilder<Trace> windows (Window[] windows) {

    this.windows = windows;

    return this;
  }

  /**
   * Builds a {@link Trace} meter using the configured time unit and window definitions.
   *
   * @param clock the registry clock forwarded to the {@link Trace} for EWMA decay timing
   * @return a fully configured {@link Trace} instance
   */
  @Override
  public Trace build (Clock clock) {

    return new Trace(clock, windowTimeUnit, windows);
  }
}
