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
 * Abstract {@link ParaboxElement} for native platform components that require placement along both axes,
 * validating at construction that min &le; preferred &le; max holds for every {@link Bias}.
 *
 * @param <C> the platform component type
 */
public abstract class ComponentParaboxElement<C> extends ParaboxElement<C> implements PlanarPart {

  /**
   * Wraps the given component and validates that its minimum, preferred, and maximum measurements are
   * non-decreasing for each axis.
   *
   * @param component  the native platform component to wrap
   * @param constraint the grow/shrink constraint governing the component's sizing
   * @throws LayoutException if minimum exceeds preferred, or preferred exceeds maximum, along any axis
   */
  public ComponentParaboxElement (C component, Constraint constraint) {

    super(component, constraint);

    for (Bias bias : Bias.values()) {

      double minimumMeasurement;
      double preferredMeasurement;
      double maximumMeasurement;

      if ((minimumMeasurement = getMinimumMeasurement(bias, null)) > (preferredMeasurement = getPreferredMeasurement(bias, null))) {
        throw new LayoutException("Layout component(%s) must yield min(%.2f)<=pref(%.2f) along the bias(%s)", component, minimumMeasurement, preferredMeasurement, bias.name());
      }
      if (preferredMeasurement > (maximumMeasurement = getMaximumMeasurement(bias, null))) {
        throw new LayoutException("Layout component(%s) must yield pref(%.2f)<=max(%.2f) along the bias(%s)", component, preferredMeasurement, maximumMeasurement, bias.name());
      }
    }
  }

  /**
   * Returns the intrinsic minimum measurement of the wrapped component along the given axis.
   *
   * @param bias the axis of measurement
   * @return the component's minimum size
   */
  public abstract double getComponentMinimumMeasurement (Bias bias);

  /**
   * Returns the intrinsic preferred measurement of the wrapped component along the given axis.
   *
   * @param bias the axis of measurement
   * @return the component's preferred size
   */
  public abstract double getComponentPreferredMeasurement (Bias bias);

  /**
   * Returns the intrinsic maximum measurement of the wrapped component along the given axis.
   *
   * @param bias the axis of measurement
   * @return the component's maximum size
   */
  public abstract double getComponentMaximumMeasurement (Bias bias);

  /**
   * Returns {@code true} because this element wraps a native platform component.
   *
   * @return {@code true}
   */
  @Override
  public boolean isNativeComponent () {

    return true;
  }

  /**
   * Returns {@link Dimensionality#PLANE} because native components are positioned along both axes.
   *
   * @return {@link Dimensionality#PLANE}
   */
  @Override
  public Dimensionality getDimensionality () {

    return Dimensionality.PLANE;
  }

  /**
   * Returns the component's minimum measurement along the given axis, ignoring the tailor.
   *
   * @param bias   the axis of measurement
   * @param tailor unused; native component sizes come directly from the component
   * @return the component's minimum size
   */
  @Override
  public double getPartMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return getComponentMinimumMeasurement(bias);
  }

  /**
   * Returns the component's preferred measurement along the given axis, ignoring the tailor.
   *
   * @param bias   the axis of measurement
   * @param tailor unused; native component sizes come directly from the component
   * @return the component's preferred size
   */
  @Override
  public double getPartPreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return getComponentPreferredMeasurement(bias);
  }

  /**
   * Returns the component's maximum measurement along the given axis, ignoring the tailor.
   *
   * @param bias   the axis of measurement
   * @param tailor unused; native component sizes come directly from the component
   * @return the component's maximum size
   */
  @Override
  public double getPartMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return getComponentMaximumMeasurement(bias);
  }
}
