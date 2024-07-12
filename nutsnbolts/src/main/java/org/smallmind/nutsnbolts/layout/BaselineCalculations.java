/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.util.List;

public class BaselineCalculations {

  private final Pair[] elementAscentsDescents;
  private final double idealizedBaseline;

  public BaselineCalculations (Bias bias, Double maximumOverrideMeasurement, double containerMeasurement, List<ParaboxElement<?>> elements, LayoutTailor tailor) {

    elementAscentsDescents = new Pair[elements.size()];
    double maxAscent = 0;
    double maxDescent = 0;
    int index = 0;

    for (ParaboxElement<?> element : elements) {

      double currentMeasurement = Math.min(containerMeasurement, (maximumOverrideMeasurement != null) ? maximumOverrideMeasurement : element.getMaximumMeasurement(bias, tailor));
      double currentAscent = element.getBaseline(bias, currentMeasurement);
      double currentDescent;

      if (currentAscent > maxAscent) {
        maxAscent = currentAscent;
      }
      if ((currentDescent = (currentMeasurement - currentAscent)) > maxDescent) {
        maxDescent = currentDescent;
      }

      elementAscentsDescents[index++] = new Pair(currentAscent, currentDescent);
    }

    idealizedBaseline = maxAscent + (Math.max(0, containerMeasurement - (maxAscent + maxDescent)) / 2);
  }

  public double getIdealizedBaseline () {

    return idealizedBaseline;
  }

  public Pair[] getElementAscentsDescents () {

    return elementAscentsDescents;
  }
}
