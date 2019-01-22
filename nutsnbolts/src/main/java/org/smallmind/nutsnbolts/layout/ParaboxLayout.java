/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

import java.util.Arrays;
import java.util.List;

public class ParaboxLayout {

  private final ParaboxContainer container;
  private final Perimeter perimeter;

  private Box horizontalBox;
  private Box verticalBox;

  public ParaboxLayout (ParaboxContainer container) {

    this(container.getPlatform().getFramePerimeter(), container);
  }

  public ParaboxLayout (Perimeter perimeter, ParaboxContainer container) {

    this.perimeter = perimeter;
    this.container = container;
  }

  public ParaboxContainer getContainer () {

    return container;
  }

  public Box<?> getHorizontalBox () {

    return horizontalBox;
  }

  public ParaboxLayout setHorizontalBox (Box<?> horizontalBox) {

    this.horizontalBox = horizontalBox;

    return this;
  }

  public Box<?> getVerticalBox () {

    return verticalBox;
  }

  public ParaboxLayout setVerticalBox (Box<?> verticalBox) {

    this.verticalBox = verticalBox;

    return this;
  }

  public void removeAll () {

    horizontalBox.removeAll();
    verticalBox.removeAll();
  }

  public void remove (Object object) {

    horizontalBox.remove(object);
    verticalBox.remove(object);
  }

  public double calculateMinimumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMinimumMeasurement(Bias.HORIZONTAL, null);
  }

  public double calculateMinimumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMinimumMeasurement(Bias.VERTICAL, null);
  }

  public Pair calculateMinimumSize () {

    return new Pair(calculateMinimumWidth(), calculateMinimumHeight());
  }

  public double calculatePreferredWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculatePreferredMeasurement(Bias.HORIZONTAL, null);
  }

  public double calculatePreferredHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculatePreferredMeasurement(Bias.VERTICAL, null);
  }

  public Pair calculatePreferredSize () {

    return new Pair(calculatePreferredWidth(), calculatePreferredHeight());
  }

  public double calculateMaximumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMaximumMeasurement(Bias.HORIZONTAL, null);
  }

  public double calculateMaximumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMaximumMeasurement(Bias.VERTICAL, null);
  }

  public Pair calculateMaximumSize () {

    return new Pair(calculateMaximumWidth(), calculateMaximumHeight());
  }

  private double getHorizontalPerimeterRequirements () {

    return perimeter.getLeft() + perimeter.getRight();
  }

  private double getVerticalPerimeterRequirements () {

    return perimeter.getTop() + perimeter.getBottom();
  }

  public void doLayout (double width, double height, Object... components) {

    doLayout(width, height, Arrays.asList(components));
  }

  public void doLayout (double width, double height, List componentList) {

    LayoutTailor tailor;

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }
    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    horizontalBox.doLayout(Bias.HORIZONTAL, perimeter.getLeft(), width - getHorizontalPerimeterRequirements(), tailor = new LayoutTailor(componentList));
    verticalBox.doLayout(Bias.VERTICAL, perimeter.getTop(), height - getVerticalPerimeterRequirements(), tailor);

    tailor.cleanup();
  }

  public ParallelBox parallelBox () {

    return new ParallelBox(this);
  }

  public ParallelBox parallelBox (Alignment alignment) {

    return new ParallelBox(this, alignment);
  }

  public SerialBox serialBox () {

    return new SerialBox(this);
  }

  public SerialBox serialBox (boolean greedy) {

    return new SerialBox(this, greedy);
  }

  public SerialBox serialBox (Gap gap) {

    return new SerialBox(this, gap);
  }

  public SerialBox serialBox (Gap gap, boolean greedy) {

    return new SerialBox(this, gap, greedy);
  }

  public SerialBox serialBox (double gap) {

    return new SerialBox(this, gap);
  }

  public SerialBox serialBox (double gap, boolean greedy) {

    return new SerialBox(this, gap, greedy);
  }

  public SerialBox serialBox (Justification justification) {

    return new SerialBox(this, justification);
  }

  public SerialBox serialBox (Justification justification, boolean greedy) {

    return new SerialBox(this, justification, greedy);
  }

  public SerialBox serialBox (Gap gap, Justification justification) {

    return new SerialBox(this, gap, justification);
  }

  public SerialBox serialBox (Gap gap, Justification justification, boolean greedy) {

    return new SerialBox(this, gap, justification, greedy);
  }

  public SerialBox serialBox (double gap, Justification justification) {

    return new SerialBox(this, gap, justification);
  }

  public SerialBox serialBox (double gap, Justification justification, boolean greedy) {

    return new SerialBox(this, gap, justification, greedy);
  }
}