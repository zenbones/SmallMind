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

import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.Window;
import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;

/**
 * JSON-mapped configuration properties for a trace meter. Instances of this class are
 * populated by the JSON doppelganger framework and subsequently consumed by
 * {@link TraceParser} to build a configured trace meter. Default values mirror the
 * typical exponentially-weighted moving-average windows (1-, 5-, and 15-minute) used
 * by many monitoring systems.
 */
@Doppelganger(namespace = "http://org.smallmind/claxon/registry")
public class TraceProperties {

  /**
   * The {@link TimeUnit} applied to all {@link Window} values when constructing the trace
   * meter. Defaults to {@link TimeUnit#MINUTES}. Serialized and deserialized via
   * {@link TimeUnitEnumXmlAdapter}.
   */
  @View(adapter = TimeUnitEnumXmlAdapter.class, idioms = @Idiom(visibility = IN))
  private TimeUnit windowTimeUnit = TimeUnit.MINUTES;

  /**
   * The set of named time windows for which the trace meter maintains moving-average
   * statistics. Defaults to three standard windows: {@code m1} (1 minute),
   * {@code m5} (5 minutes), and {@code m15} (15 minutes).
   */
  @View(idioms = @Idiom(visibility = IN))
  private Window[] windows = new Window[] {new Window("m1", 1), new Window("m5", 5), new Window("m15", 15)};

  /**
   * Returns the time unit applied to all window value definitions.
   *
   * @return the configured {@link TimeUnit}; never {@code null} unless explicitly set to {@code null}
   */
  public TimeUnit getWindowTimeUnit () {

    return windowTimeUnit;
  }

  /**
   * Sets the time unit applied to all window value definitions.
   *
   * @param windowTimeUnit the {@link TimeUnit} to use for window durations; {@code null} retains
   *                       the builder default
   */
  public void setWindowTimeUnit (TimeUnit windowTimeUnit) {

    this.windowTimeUnit = windowTimeUnit;
  }

  /**
   * Returns the array of named time windows configured for the trace meter.
   *
   * @return array of {@link Window} definitions; never {@code null} unless explicitly set to {@code null}
   */
  public Window[] getWindows () {

    return windows;
  }

  /**
   * Replaces the set of named time windows for the trace meter.
   *
   * @param windows array of {@link Window} definitions; {@code null} retains the builder default
   */
  public void setWindows (Window[] windows) {

    this.windows = windows;
  }
}
