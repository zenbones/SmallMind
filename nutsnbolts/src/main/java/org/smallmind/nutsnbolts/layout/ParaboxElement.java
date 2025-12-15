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
package org.smallmind.nutsnbolts.layout;

/**
 * Base wrapper around a component or nested box used by {@link ParaboxLayout}. Provides cached
 * access to sizing measurements through an optional {@link LayoutTailor}.
 *
 * @param <P> the underlying part type
 */
public abstract class ParaboxElement<P> {

  private final P part;
  private final Constraint constraint;

  /**
   * Creates a new element wrapper.
   *
   * @param part       the wrapped component or box
   * @param constraint sizing and alignment constraint
   */
  public ParaboxElement (P part, Constraint constraint) {

    this.part = part;
    this.constraint = constraint;
  }

  /**
   * @return {@code true} if this element represents a native platform component
   */
  public abstract boolean isNativeComponent ();

  /**
   * @return the dimensionality of the wrapped part
   */
  public abstract Dimensionality getDimensionality ();

  /**
   * Returns the raw minimum measurement of the part along the axis.
   *
   * @param bias   the axis of measurement
   * @param tailor optional layout tailor for recursive measurement
   * @return the minimum size
   */
  public abstract double getPartMinimumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the raw preferred measurement of the part along the axis.
   *
   * @param bias   the axis of measurement
   * @param tailor optional layout tailor for recursive measurement
   * @return the preferred size
   */
  public abstract double getPartPreferredMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the raw maximum measurement of the part along the axis.
   *
   * @param bias   the axis of measurement
   * @param tailor optional layout tailor for recursive measurement
   * @return the maximum size
   */
  public abstract double getPartMaximumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the baseline position for the part given a specific measurement.
   *
   * @param bias        the axis of measurement
   * @param measurement the allocated size along the axis
   * @return the baseline offset
   */
  public abstract double getBaseline (Bias bias, double measurement);

  /**
   * Returns the wrapped part.
   *
   * @return the part
   */
  public P getPart () {

    return part;
  }

  /**
   * Returns the constraint governing this element.
   *
   * @return the constraint
   */
  public Constraint getConstraint () {

    return constraint;
  }

  /**
   * Returns the minimum measurement honoring constraint behavior and caching through the tailor.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor cache (may be {@code null})
   * @return the minimum size
   */
  public double getMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    if (tailor == null) {

      return (constraint.getShrink() > 0) ? getPartMinimumMeasurement(bias, tailor) : getPartPreferredMeasurement(bias, tailor);
    } else {

      Double measurement;

      if ((measurement = tailor.lookup(part, bias, TapeMeasure.MINIMUM)) == null) {
        tailor.store(part, bias, TapeMeasure.MINIMUM, measurement = (constraint.getShrink() > 0) ? getPartMinimumMeasurement(bias, tailor) : getPartPreferredMeasurement(bias, tailor));
      }

      return measurement;
    }
  }

  /**
   * Returns the preferred measurement, caching via the tailor when provided.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor cache (may be {@code null})
   * @return the preferred size
   */
  public double getPreferredMeasurement (Bias bias, LayoutTailor tailor) {

    if (tailor == null) {

      return getPartPreferredMeasurement(bias, tailor);
    } else {

      Double measurement;

      if ((measurement = tailor.lookup(part, bias, TapeMeasure.PREFERRED)) == null) {
        tailor.store(part, bias, TapeMeasure.PREFERRED, measurement = getPartPreferredMeasurement(bias, tailor));
      }

      return measurement;
    }
  }

  /**
   * Returns the maximum measurement honoring constraint behavior and caching through the tailor.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor cache (may be {@code null})
   * @return the maximum size
   */
  public double getMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    if (tailor == null) {

      return (constraint.getGrow() > 0) ? getPartMaximumMeasurement(bias, tailor) : getPartPreferredMeasurement(bias, tailor);
    } else {

      Double measurement;

      if ((measurement = tailor.lookup(part, bias, TapeMeasure.MAXIMUM)) == null) {
        tailor.store(part, bias, TapeMeasure.MAXIMUM, measurement = (constraint.getGrow() > 0) ? getPartMaximumMeasurement(bias, tailor) : getPartPreferredMeasurement(bias, tailor));
      }

      return measurement;
    }
  }
}
