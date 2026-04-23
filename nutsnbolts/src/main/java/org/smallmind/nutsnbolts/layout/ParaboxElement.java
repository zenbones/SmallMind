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
 * Abstract base that wraps a layout part (either a native component or a nested {@link Box}) together
 * with its {@link Constraint}, and provides tailor-cached access to minimum, preferred, and maximum
 * measurements for use during measurement and layout passes.
 *
 * @param <P> the type of the wrapped part
 */
public abstract class ParaboxElement<P> {

  private final P part;
  private final Constraint constraint;

  /**
   * Constructs an element wrapping the given part with the specified sizing constraint.
   *
   * @param part       the wrapped component or nested box
   * @param constraint the grow/shrink constraint governing this element's sizing
   */
  public ParaboxElement (P part, Constraint constraint) {

    this.part = part;
    this.constraint = constraint;
  }

  /**
   * Returns whether this element wraps a native platform component rather than a nested box.
   *
   * @return {@code true} for native components; {@code false} for nested boxes
   */
  public abstract boolean isNativeComponent ();

  /**
   * Returns whether this element requires single-axis ({@link Dimensionality#LINE}) or
   * two-axis ({@link Dimensionality#PLANE}) layout coordination.
   *
   * @return the dimensionality of the wrapped part
   */
  public abstract Dimensionality getDimensionality ();

  /**
   * Returns the intrinsic minimum measurement of the wrapped part along the given axis,
   * without applying constraint-based adjustments.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement; may be {@code null}
   * @return the part's raw minimum size
   */
  public abstract double getPartMinimumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the intrinsic preferred measurement of the wrapped part along the given axis,
   * without applying constraint-based adjustments.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement; may be {@code null}
   * @return the part's raw preferred size
   */
  public abstract double getPartPreferredMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the intrinsic maximum measurement of the wrapped part along the given axis,
   * without applying constraint-based adjustments.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement; may be {@code null}
   * @return the part's raw maximum size
   */
  public abstract double getPartMaximumMeasurement (Bias bias, LayoutTailor tailor);

  /**
   * Returns the baseline offset of the wrapped part for a given allocated size along the axis,
   * used for {@link Alignment#BASELINE} calculations.
   *
   * @param bias        the axis of measurement
   * @param measurement the size allocated to the part along the axis
   * @return the distance from the leading edge of the part to its text baseline
   */
  public abstract double getBaseline (Bias bias, double measurement);

  /**
   * Returns the wrapped component or nested box.
   *
   * @return the part managed by this element
   */
  public P getPart () {

    return part;
  }

  /**
   * Returns the {@link Constraint} that governs how this element grows or shrinks during layout.
   *
   * @return this element's constraint
   */
  public Constraint getConstraint () {

    return constraint;
  }

  /**
   * Returns the effective minimum measurement for this element, applying the constraint's shrink behavior:
   * if the element may shrink, returns the part's true minimum; otherwise returns the preferred size.
   * Results are cached through the tailor when one is provided.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for caching; {@code null} disables caching
   * @return the effective minimum size along the axis
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
   * Returns the preferred measurement of this element, caching the result through the tailor
   * when one is provided.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for caching; {@code null} disables caching
   * @return the preferred size along the axis
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
   * Returns the effective maximum measurement for this element, applying the constraint's grow behavior:
   * if the element may grow, returns the part's true maximum; otherwise returns the preferred size.
   * Results are cached through the tailor when one is provided.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for caching; {@code null} disables caching
   * @return the effective maximum size along the axis
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
