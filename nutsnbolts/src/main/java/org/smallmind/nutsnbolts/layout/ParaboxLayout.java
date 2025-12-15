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

import java.util.Arrays;
import java.util.List;

/**
 * Coordinates horizontal and vertical {@link Box} instances to lay out components with platform-specific
 * perimeter insets. Provides measurement calculations and factories for creating serial or parallel boxes.
 */
public class ParaboxLayout {

  private final ParaboxContainer container;
  private final Perimeter perimeter;

  private Box<?> horizontalBox;
  private Box<?> verticalBox;

  /**
   * Creates a layout using the container's frame perimeter.
   *
   * @param container the platform container to manage
   */
  public ParaboxLayout (ParaboxContainer container) {

    this(container.getPlatform().getFramePerimeter(), container);
  }

  /**
   * Creates a layout with an explicit perimeter.
   *
   * @param perimeter frame perimeter to respect
   * @param container the platform container to manage
   */
  public ParaboxLayout (Perimeter perimeter, ParaboxContainer container) {

    this.perimeter = perimeter;
    this.container = container;
  }

  /**
   * @return the container hosting laid out components
   */
  public ParaboxContainer getContainer () {

    return container;
  }

  /**
   * @return the horizontal box, if set
   */
  public Box<?> getHorizontalBox () {

    return horizontalBox;
  }

  /**
   * Sets the horizontal box used for layout.
   *
   * @param horizontalBox the box managing horizontal sizing and positioning
   * @return this layout for chaining
   */
  public ParaboxLayout setHorizontalBox (Box<?> horizontalBox) {

    this.horizontalBox = horizontalBox;

    return this;
  }

  /**
   * @return the vertical box, if set
   */
  public Box<?> getVerticalBox () {

    return verticalBox;
  }

  /**
   * Sets the vertical box used for layout.
   *
   * @param verticalBox the box managing vertical sizing and positioning
   * @return this layout for chaining
   */
  public ParaboxLayout setVerticalBox (Box<?> verticalBox) {

    this.verticalBox = verticalBox;

    return this;
  }

  /**
   * Removes all components from both horizontal and vertical boxes.
   */
  public void removeAll () {

    horizontalBox.removeAll();
    verticalBox.removeAll();
  }

  /**
   * Removes a component from both horizontal and vertical boxes.
   *
   * @param object the component to remove
   */
  public void remove (Object object) {

    horizontalBox.remove(object);
    verticalBox.remove(object);
  }

  /**
   * Calculates the minimum width including perimeter requirements.
   *
   * @return the minimum width
   * @throws LayoutException if the horizontal box has not been set
   */
  public double calculateMinimumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMinimumMeasurement(Bias.HORIZONTAL, null);
  }

  /**
   * Calculates the minimum height including perimeter requirements.
   *
   * @return the minimum height
   * @throws LayoutException if the vertical box has not been set
   */
  public double calculateMinimumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMinimumMeasurement(Bias.VERTICAL, null);
  }

  /**
   * Calculates the minimum size along both axes.
   *
   * @return width/height pair
   */
  public Pair calculateMinimumSize () {

    return new Pair(calculateMinimumWidth(), calculateMinimumHeight());
  }

  /**
   * Calculates the preferred width including perimeter requirements.
   *
   * @return the preferred width
   * @throws LayoutException if the horizontal box has not been set
   */
  public double calculatePreferredWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculatePreferredMeasurement(Bias.HORIZONTAL, null);
  }

  /**
   * Calculates the preferred height including perimeter requirements.
   *
   * @return the preferred height
   * @throws LayoutException if the vertical box has not been set
   */
  public double calculatePreferredHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculatePreferredMeasurement(Bias.VERTICAL, null);
  }

  /**
   * Calculates the preferred size along both axes.
   *
   * @return width/height pair
   */
  public Pair calculatePreferredSize () {

    return new Pair(calculatePreferredWidth(), calculatePreferredHeight());
  }

  /**
   * Calculates the maximum width including perimeter requirements.
   *
   * @return the maximum width
   * @throws LayoutException if the horizontal box has not been set
   */
  public double calculateMaximumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMaximumMeasurement(Bias.HORIZONTAL, null);
  }

  /**
   * Calculates the maximum height including perimeter requirements.
   *
   * @return the maximum height
   * @throws LayoutException if the vertical box has not been set
   */
  public double calculateMaximumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMaximumMeasurement(Bias.VERTICAL, null);
  }

  /**
   * Calculates the maximum size along both axes.
   *
   * @return width/height pair
   */
  public Pair calculateMaximumSize () {

    return new Pair(calculateMaximumWidth(), calculateMaximumHeight());
  }

  private double getHorizontalPerimeterRequirements () {

    return perimeter.left() + perimeter.right();
  }

  private double getVerticalPerimeterRequirements () {

    return perimeter.top() + perimeter.bottom();
  }

  /**
   * Performs layout for the provided components using a horizontal and vertical box.
   *
   * @param width      the available width
   * @param height     the available height
   * @param components components to lay out
   */
  public void doLayout (double width, double height, Object... components) {

    doLayout(width, height, Arrays.asList(components));
  }

  /**
   * Performs layout for the provided components list using a horizontal and vertical box.
   *
   * @param width         the available width
   * @param height        the available height
   * @param componentList the components to lay out
   * @throws LayoutException if either horizontal or vertical box is unset or components are misconfigured
   */
  public void doLayout (double width, double height, List<?> componentList) {

    LayoutTailor tailor;

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }
    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    horizontalBox.doLayout(Bias.HORIZONTAL, perimeter.left(), width - getHorizontalPerimeterRequirements(), tailor = new LayoutTailor(componentList));
    verticalBox.doLayout(Bias.VERTICAL, perimeter.top(), height - getVerticalPerimeterRequirements(), tailor);

    tailor.cleanup();
  }

  /**
   * Factory for creating a parallel box with default alignment.
   *
   * @return a new parallel box
   */
  public ParallelBox parallelBox () {

    return new ParallelBox(this);
  }

  /**
   * Factory for creating a parallel box with the given alignment.
   *
   * @param alignment alignment to apply
   * @return a new parallel box
   */
  public ParallelBox parallelBox (Alignment alignment) {

    return new ParallelBox(this, alignment);
  }

  /**
   * Factory for creating a serial box with default settings.
   *
   * @return a new serial box
   */
  public SerialBox serialBox () {

    return new SerialBox(this);
  }

  /**
   * Factory for creating a serial box configured for greediness.
   *
   * @param greedy whether to greedily consume space
   * @return a new serial box
   */
  public SerialBox serialBox (boolean greedy) {

    return new SerialBox(this, greedy);
  }

  /**
   * Factory for creating a serial box with a gap.
   *
   * @param gap the gap type between elements
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap) {

    return new SerialBox(this, gap);
  }

  /**
   * Factory for creating a serial box with a gap and greediness flag.
   *
   * @param gap    the gap type between elements
   * @param greedy whether to greedily consume space
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap, boolean greedy) {

    return new SerialBox(this, gap, greedy);
  }

  /**
   * Factory for creating a serial box with a fixed gap.
   *
   * @param gap the fixed gap value
   * @return a new serial box
   */
  public SerialBox serialBox (double gap) {

    return new SerialBox(this, gap);
  }

  /**
   * Factory for creating a serial box with a fixed gap and greediness flag.
   *
   * @param gap    the fixed gap value
   * @param greedy whether to greedily consume space
   * @return a new serial box
   */
  public SerialBox serialBox (double gap, boolean greedy) {

    return new SerialBox(this, gap, greedy);
  }

  /**
   * Factory for creating a serial box with justification.
   *
   * @param justification space distribution strategy
   * @return a new serial box
   */
  public SerialBox serialBox (Justification justification) {

    return new SerialBox(this, justification);
  }

  /**
   * Factory for creating a serial box with justification and greediness flag.
   *
   * @param justification space distribution strategy
   * @param greedy        whether to greedily consume space
   * @return a new serial box
   */
  public SerialBox serialBox (Justification justification, boolean greedy) {

    return new SerialBox(this, justification, greedy);
  }

  /**
   * Factory for creating a serial box with gap and justification.
   *
   * @param gap           the gap type between elements
   * @param justification space distribution strategy
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap, Justification justification) {

    return new SerialBox(this, gap, justification);
  }

  /**
   * Factory for creating a serial box with gap, justification, and greediness flag.
   *
   * @param gap           the gap type between elements
   * @param justification space distribution strategy
   * @param greedy        whether to greedily consume space
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap, Justification justification, boolean greedy) {

    return new SerialBox(this, gap, justification, greedy);
  }

  /**
   * Factory for creating a serial box with a fixed gap and justification.
   *
   * @param gap           the fixed gap value
   * @param justification space distribution strategy
   * @return a new serial box
   */
  public SerialBox serialBox (double gap, Justification justification) {

    return new SerialBox(this, gap, justification);
  }

  /**
   * Factory for creating a serial box with a fixed gap, justification, and greediness flag.
   *
   * @param gap           the fixed gap value
   * @param justification space distribution strategy
   * @param greedy        whether to greedily consume space
   * @return a new serial box
   */
  public SerialBox serialBox (double gap, Justification justification, boolean greedy) {

    return new SerialBox(this, gap, justification, greedy);
  }
}
