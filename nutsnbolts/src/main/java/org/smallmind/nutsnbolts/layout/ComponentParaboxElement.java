/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.layout;

public abstract class ComponentParaboxElement<C> extends ParaboxElement<C> implements PlanarPart {

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

  public abstract double getComponentMinimumMeasurement (Bias bias);

  public abstract double getComponentPreferredMeasurement (Bias bias);

  public abstract double getComponentMaximumMeasurement (Bias bias);

  @Override
  public boolean isNativeComponent () {

    return true;
  }

  @Override
  public Dimensionality getDimensionality () {

    return Dimensionality.PLANE;
  }

  @Override
  public double getPartMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return getComponentMinimumMeasurement(bias);
  }

  @Override
  public double getPartPreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return getComponentPreferredMeasurement(bias);
  }

  @Override
  public double getPartMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return getComponentMaximumMeasurement(bias);
  }
}
