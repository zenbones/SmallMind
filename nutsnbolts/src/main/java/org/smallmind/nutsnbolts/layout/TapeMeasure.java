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
 * Identifies which of the three sizing measurements — minimum, preferred, or maximum — should be
 * retrieved from a {@link ParaboxElement} during measurement and layout passes.
 */
public enum TapeMeasure {

  /**
   * Retrieves the minimum measurement, reflecting the smallest size at which the element remains usable.
   */
  MINIMUM {
    /**
     * Returns the element's effective minimum measurement along the given axis.
     *
     * @param bias    the axis of measurement
     * @param element the element to measure
     * @param tailor  the layout tailor used for caching
     * @return the minimum size
     */
    @Override
    public double getMeasure (Bias bias, ParaboxElement<?> element, LayoutTailor tailor) {

      return element.getMinimumMeasurement(bias, tailor);
    }
  },

  /**
   * Retrieves the preferred measurement, reflecting the element's ideal natural size.
   */
  PREFERRED {
    /**
     * Returns the element's preferred measurement along the given axis.
     *
     * @param bias    the axis of measurement
     * @param element the element to measure
     * @param tailor  the layout tailor used for caching
     * @return the preferred size
     */
    @Override
    public double getMeasure (Bias bias, ParaboxElement<?> element, LayoutTailor tailor) {

      return element.getPreferredMeasurement(bias, tailor);
    }
  },

  /**
   * Retrieves the maximum measurement, reflecting the largest size the element should occupy.
   */
  MAXIMUM {
    /**
     * Returns the element's effective maximum measurement along the given axis.
     *
     * @param bias    the axis of measurement
     * @param element the element to measure
     * @param tailor  the layout tailor used for caching
     * @return the maximum size
     */
    @Override
    public double getMeasure (Bias bias, ParaboxElement<?> element, LayoutTailor tailor) {

      return element.getMaximumMeasurement(bias, tailor);
    }
  };

  /**
   * Retrieves the appropriate measurement from the given element along the specified axis.
   *
   * @param bias    the axis of measurement
   * @param element the element whose measurement is requested
   * @param tailor  the layout tailor used to cache repeated measurements
   * @return the measurement value corresponding to this constant
   */
  public abstract double getMeasure (Bias bias, ParaboxElement<?> element, LayoutTailor tailor);
}
