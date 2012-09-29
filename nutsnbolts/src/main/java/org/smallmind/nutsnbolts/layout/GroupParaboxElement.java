/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
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

public class GroupParaboxElement<G extends Group> extends ParaboxElement<G> {

  public GroupParaboxElement (G group, Spec spec) {

    this(group, spec.staticConstraint());
  }

  public GroupParaboxElement (G group, ParaboxConstraint constraint) {

    super(group, constraint);
  }

  @Override
  public Dimensionality getDimensionality () {

    return Dimensionality.LINE;
  }

  @Override
  public double getComponentMinimumMeasurement (Bias bias) {

    return getPart().calculateMinimumMeasurement();
  }

  @Override
  public double getComponentPreferredMeasurement (Bias bias) {

    return getPart().calculatePreferredMeasurement();
  }

  @Override
  public double getComponentMaximumMeasurement (Bias bias) {

    return getPart().calculateMaximumMeasurement();
  }

  @Override
  public double getBaseline (Bias bias, double measurement) {

    return measurement;
  }

  public void applyLayout (Bias bias, double position, double measurement, LayoutTailor tailor) {

    getPart().doLayout(position, measurement, tailor);
  }
}
