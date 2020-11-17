/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public abstract class ParaboxElement<P> {

  private final P part;
  private final Constraint constraint;

  public ParaboxElement (P part, Constraint constraint) {

    this.part = part;
    this.constraint = constraint;
  }

  public abstract boolean isNativeComponent ();

  public abstract Dimensionality getDimensionality ();

  public abstract double getPartMinimumMeasurement (Bias bias, LayoutTailor tailor);

  public abstract double getPartPreferredMeasurement (Bias bias, LayoutTailor tailor);

  public abstract double getPartMaximumMeasurement (Bias bias, LayoutTailor tailor);

  public abstract double getBaseline (Bias bias, double measurement);

  public P getPart () {

    return part;
  }

  public Constraint getConstraint () {

    return constraint;
  }

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
