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

import java.util.List;

public class ParaboxLayout {

  private ParaboxContainer container;
  private Group horizontalGroup;
  private Group verticalGroup;

  public ParaboxLayout (ParaboxContainer container) {

    this.container = container;
  }

  public Group getHorizontalGroup () {

    return horizontalGroup;
  }

  public ParaboxLayout setHorizontalGroup (Group horizontalGroup) {

    this.horizontalGroup = horizontalGroup;

    return this;
  }

  public Group getVerticalGroup () {

    return verticalGroup;
  }

  public ParaboxLayout setVerticalGroup (Group verticalGroup) {

    this.verticalGroup = verticalGroup;

    return this;
  }

  public Pair calculateMinimumContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculateBiasedContainerMeasurement(TapeMeasure.MINIMUM, elements), calculateUnbiasedContainerMeasurement(TapeMeasure.MINIMUM, minimumUnbiasedMeasurement, elements));
  }

  public Pair calculatePreferredContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculateBiasedContainerMeasurement(TapeMeasure.PREFERRED, elements), calculateUnbiasedContainerMeasurement(TapeMeasure.PREFERRED, preferredUnbiasedMeasurement, elements));
  }

  public Pair calculateMaximumContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculateBiasedContainerMeasurement(TapeMeasure.MAXIMUM, elements), calculateUnbiasedContainerMeasurement(TapeMeasure.MAXIMUM, maximumUnbiasedMeasurement, elements));
  }

  public void doLayout (double width, double height, List<ParaboxElement<?>> elements) {

    if (horizontalGroup == null) {
      throw new LayoutException("No horizontal group has been set on this layout");
    }
    if (verticalGroup == null) {
      throw new LayoutException("No vertical group has been set on this layout");
    }

    horizontalGroup.doLayout(0, width);
    verticalGroup.doLayout(0, height);
  }
}
