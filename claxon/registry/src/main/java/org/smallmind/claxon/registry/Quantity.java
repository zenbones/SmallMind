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

/**
 * Represents a single named, typed numeric measurement produced by a meter. Quantities are
 * the fundamental data unit passed from meters to emitters. Each quantity carries a logical
 * {@code name} that identifies the measurement within its meter (e.g., {@code "count"},
 * {@code "mean"}, {@code "p99"}), a {@code value} holding the numeric reading, and an
 * optional {@link QuantityType} that allows emitters to apply type-specific handling.
 */
public class Quantity {

  /**
   * Classification that allows emitters to apply type-specific processing.
   */
  private final QuantityType type;

  /**
   * Logical name identifying this measurement within its meter.
   */
  private final String name;

  /**
   * The numeric value of the measurement.
   */
  private final double value;

  /**
   * Constructs a quantity without a specific type classification, using
   * {@link QuantityType#NONE} as the default type.
   *
   * @param name  logical name identifying this measurement within its meter
   * @param value the numeric value of the measurement
   */
  public Quantity (String name, double value) {

    this(name, value, QuantityType.NONE);
  }

  /**
   * Constructs a quantity with an explicit type classification.
   *
   * @param name  logical name identifying this measurement within its meter
   * @param value the numeric value of the measurement
   * @param type  {@link QuantityType} classification enabling type-specific emitter handling
   */
  public Quantity (String name, double value, QuantityType type) {

    this.name = name;
    this.value = value;
    this.type = type;
  }

  /**
   * Returns the logical name of this measurement.
   *
   * @return the measurement name (e.g., {@code "count"}, {@code "mean"})
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the numeric value of this measurement.
   *
   * @return the measured value
   */
  public double getValue () {

    return value;
  }

  /**
   * Returns the type classification of this measurement.
   *
   * @return the {@link QuantityType} indicating how this measurement should be interpreted
   */
  public QuantityType getType () {

    return type;
  }
}
