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
package org.smallmind.claxon.registry;

import org.smallmind.web.json.doppelganger.Doppelganger;
import org.smallmind.web.json.doppelganger.Idiom;
import org.smallmind.web.json.doppelganger.View;

import static org.smallmind.web.json.doppelganger.Visibility.IN;

/**
 * Represents a named percentile measurement used within histogram and other statistical
 * meters. The {@code name} field provides a human-readable label (e.g., {@code "p95"},
 * {@code "p99"}) while the {@code value} field holds the corresponding fractional quantile
 * (e.g., {@code 0.95}, {@code 0.99}). Instances are typically configured via JSON and
 * passed to a meter builder to govern which quantiles are emitted.
 */
@Doppelganger(namespace = "http://org.smallmind/claxon/registry")
public class Percentile {

  /**
   * Human-readable label for this percentile (e.g., {@code "p95"}).
   */
  @View(idioms = @Idiom(visibility = IN))
  private String name;

  /**
   * Fractional quantile value in the range [0.0, 1.0] (e.g., {@code 0.95} for the 95th percentile).
   */
  @View(idioms = @Idiom(visibility = IN))
  private double value;

  /**
   * No-argument constructor required by the JSON doppelganger framework for reflective
   * instantiation during deserialization.
   */
  public Percentile () {

  }

  /**
   * Constructs a percentile with the specified label and quantile value.
   *
   * @param name  human-readable label for the percentile (e.g., {@code "p95"})
   * @param value fractional quantile value in [0.0, 1.0] (e.g., {@code 0.95})
   */
  public Percentile (String name, double value) {

    this.name = name;
    this.value = value;
  }

  /**
   * Returns the human-readable label for this percentile.
   *
   * @return the percentile name (e.g., {@code "p95"})
   */
  public String getName () {

    return name;
  }

  /**
   * Sets the human-readable label for this percentile.
   *
   * @param name the percentile label (e.g., {@code "p99"})
   */
  public void setName (String name) {

    this.name = name;
  }

  /**
   * Returns the fractional quantile value for this percentile.
   *
   * @return the quantile in [0.0, 1.0] (e.g., {@code 0.95} for the 95th percentile)
   */
  public double getValue () {

    return value;
  }

  /**
   * Sets the fractional quantile value for this percentile.
   *
   * @param value the quantile in [0.0, 1.0] (e.g., {@code 0.99} for the 99th percentile)
   */
  public void setValue (double value) {

    this.value = value;
  }
}
