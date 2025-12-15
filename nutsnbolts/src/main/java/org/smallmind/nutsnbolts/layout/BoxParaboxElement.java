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
 * {@link ParaboxElement} wrapper around another {@link Box}, enabling nested layouts to be treated
 * as linear parts.
 */
public class BoxParaboxElement extends ParaboxElement<Box<?>> implements LinearPart {

  /**
   * Creates a new wrapper for the given box and constraint.
   *
   * @param box        the nested box
   * @param constraint the constraint governing the box's sizing and alignment
   */
  public BoxParaboxElement (Box<?> box, Constraint constraint) {

    super(box, constraint);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isNativeComponent () {

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Dimensionality getDimensionality () {

    return Dimensionality.LINE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getPartMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return getPart().calculateMinimumMeasurement(bias, tailor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getPartPreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return getPart().calculatePreferredMeasurement(bias, tailor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getPartMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return getPart().calculateMaximumMeasurement(bias, tailor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getBaseline (Bias bias, double measurement) {

    return measurement;
  }

  /**
   * Lays out the nested box along the provided axis using the supplied space.
   *
   * @param bias        the axis being laid out
   * @param position    the start position along the axis
   * @param measurement the available measurement along the axis
   * @param tailor      the layout tailor used to size contained elements
   */
  @Override
  public void applyLayout (Bias bias, double position, double measurement, LayoutTailor tailor) {

    getPart().doLayout(bias, position, measurement, tailor);
  }
}
