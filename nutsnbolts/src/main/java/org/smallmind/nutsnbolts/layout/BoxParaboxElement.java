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
 * A {@link ParaboxElement} that wraps a nested {@link Box}, allowing child boxes to participate
 * in measurement and layout as {@link LinearPart}s along a single axis.
 */
public class BoxParaboxElement extends ParaboxElement<Box<?>> implements LinearPart {

  /**
   * Constructs an element that wraps the given nested box with the specified constraint.
   *
   * @param box        the nested box to wrap
   * @param constraint the grow/shrink constraint applied to the nested box during layout
   */
  public BoxParaboxElement (Box<?> box, Constraint constraint) {

    super(box, constraint);
  }

  /**
   * Returns {@code false} because this element wraps a nested {@link Box}, not a native component.
   *
   * @return {@code false}
   */
  @Override
  public boolean isNativeComponent () {

    return false;
  }

  /**
   * Returns {@link Dimensionality#LINE} because nested boxes are laid out along a single axis.
   *
   * @return {@link Dimensionality#LINE}
   */
  @Override
  public Dimensionality getDimensionality () {

    return Dimensionality.LINE;
  }

  /**
   * Delegates to the nested box to compute its minimum measurement along the given axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching
   * @return the nested box's minimum size along the axis
   */
  @Override
  public double getPartMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return getPart().calculateMinimumMeasurement(bias, tailor);
  }

  /**
   * Delegates to the nested box to compute its preferred measurement along the given axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching
   * @return the nested box's preferred size along the axis
   */
  @Override
  public double getPartPreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return getPart().calculatePreferredMeasurement(bias, tailor);
  }

  /**
   * Delegates to the nested box to compute its maximum measurement along the given axis.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching
   * @return the nested box's maximum size along the axis
   */
  @Override
  public double getPartMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return getPart().calculateMaximumMeasurement(bias, tailor);
  }

  /**
   * Returns the full measurement as the baseline for a nested box, since boxes do not define a text baseline.
   *
   * @param bias        the axis of measurement (unused for boxes)
   * @param measurement the allocated size along the axis
   * @return {@code measurement}, indicating a bottom-of-box baseline
   */
  @Override
  public double getBaseline (Bias bias, double measurement) {

    return measurement;
  }

  /**
   * Triggers the nested box's layout pass for the given axis, position, and available space.
   *
   * @param bias        the axis being laid out
   * @param position    the starting offset along the axis
   * @param measurement the total space allocated to the nested box along the axis
   * @param tailor      the layout tailor coordinating the full two-axis layout pass
   */
  @Override
  public void applyLayout (Bias bias, double position, double measurement, LayoutTailor tailor) {

    getPart().doLayout(bias, position, measurement, tailor);
  }
}
