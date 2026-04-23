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

import java.util.List;

/**
 * Computes per-element ascent/descent pairs and an idealized baseline position for a group of
 * {@link ParaboxElement}s, enabling {@link Alignment#BASELINE} layout within a {@link ParallelBox}.
 */
public class BaselineCalculations {

  private final Pair[] elementAscentsDescents;
  private final double idealizedBaseline;

  /**
   * Measures each element's ascent and descent along the given axis and derives an idealized baseline
   * that centers the combined ascent-plus-descent span within the container.
   *
   * @param bias                       the axis of measurement
   * @param maximumOverrideMeasurement optional upper bound on each element's size; {@code null} defers to the element's own maximum
   * @param containerMeasurement       the total available space along the axis
   * @param elements                   the elements whose baselines are to be calculated
   * @param tailor                     the layout tailor used to retrieve element measurements
   */
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

  /**
   * Returns the idealized baseline position within the container, calculated as the maximum ascent
   * plus half of any remaining space after accounting for the tallest ascent-descent pair.
   *
   * @return the idealized baseline offset from the container's leading edge
   */
  public double getIdealizedBaseline () {

    return idealizedBaseline;
  }

  /**
   * Returns the ascent/descent {@link Pair} computed for each element, in the same order as the
   * element list passed to the constructor.
   *
   * @return array of ascent/descent pairs indexed by element position
   */
  public Pair[] getElementAscentsDescents () {

    return elementAscentsDescents;
  }
}
