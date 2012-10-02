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

import java.util.Arrays;
import java.util.List;

public class ParaboxLayout {

  private ParaboxContainer container;
  private Group horizontalGroup;
  private Group verticalGroup;

  public ParaboxLayout (ParaboxContainer container) {

    this.container = container;
  }

  public ParaboxContainer getContainer () {

    return container;
  }

  public Group<?> getHorizontalGroup () {

    return horizontalGroup;
  }

  public ParaboxLayout setHorizontalGroup (Group<?> horizontalGroup) {

    this.horizontalGroup = horizontalGroup;

    return this;
  }

  public Group<?> getVerticalGroup () {

    return verticalGroup;
  }

  public ParaboxLayout setVerticalGroup (Group<?> verticalGroup) {

    this.verticalGroup = verticalGroup;

    return this;
  }

  public Pair calculateMinimumSize () {

    return new Pair(getHorizontalPerimeterRequirements() + horizontalGroup.calculateMinimumMeasurement(Bias.HORIZONTAL, null), getVerticalPerimeterRequirements() + verticalGroup.calculateMinimumMeasurement(Bias.VERTICAL, null));
  }

  public Pair calculatePreferredSize () {

    return new Pair(getHorizontalPerimeterRequirements() + horizontalGroup.calculatePreferredMeasurement(Bias.HORIZONTAL, null), getVerticalPerimeterRequirements() + verticalGroup.calculatePreferredMeasurement(Bias.VERTICAL, null));
  }

  public Pair calculateMaximumSize () {

    return new Pair(getHorizontalPerimeterRequirements() + horizontalGroup.calculateMaximumMeasurement(Bias.HORIZONTAL, null), getVerticalPerimeterRequirements() + verticalGroup.calculateMaximumMeasurement(Bias.VERTICAL, null));
  }

  private double getHorizontalPerimeterRequirements () {

    return container.getPlatform().getFramePerimeter().getLeft() + container.getPlatform().getFramePerimeter().getRight();
  }

  private double getVerticalPerimeterRequirements () {

    return container.getPlatform().getFramePerimeter().getTop() + container.getPlatform().getFramePerimeter().getBottom();
  }

  public void doLayout (double width, double height, Object... components) {

    doLayout(width, height, Arrays.asList(components));
  }

  public void doLayout (double width, double height, List componentList) {

    LayoutTailor tailor;

    if (horizontalGroup == null) {
      throw new LayoutException("No horizontal group has been set on this layout");
    }
    if (verticalGroup == null) {
      throw new LayoutException("No vertical group has been set on this layout");
    }

    horizontalGroup.doLayout(Bias.HORIZONTAL, container.getPlatform().getFramePerimeter().getLeft(), width - getHorizontalPerimeterRequirements(), tailor = new LayoutTailor(componentList));
    verticalGroup.doLayout(Bias.VERTICAL, container.getPlatform().getFramePerimeter().getTop(), height - getVerticalPerimeterRequirements(), tailor);

    tailor.cleanup();
  }

  public ParallelGroup parallelGroup () {

    return new ParallelGroup(this);
  }

  public ParallelGroup parallelGroup (Alignment alignment) {

    return new ParallelGroup(this, alignment);
  }

  public SequentialGroup sequentialGroup () {

    return new SequentialGroup(this);
  }

  public SequentialGroup sequentialGroup (Gap gap) {

    return new SequentialGroup(this, gap);
  }

  public SequentialGroup sequentialGroup (double gap) {

    return new SequentialGroup(this, gap);
  }

  public SequentialGroup sequentialGroup (Justification justification) {

    return new SequentialGroup(this, justification);
  }

  public SequentialGroup sequentialGroup (Gap gap, Justification justification) {

    return new SequentialGroup(this, gap, justification);
  }

  public SequentialGroup sequentialGroup (double gap, Justification justification) {

    return new SequentialGroup(this, gap, justification);
  }
}