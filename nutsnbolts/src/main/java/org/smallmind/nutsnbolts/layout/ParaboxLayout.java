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
 * Central controller that pairs a horizontal {@link Box} with a vertical {@link Box} to produce
 * two-axis layout for all components in a {@link ParaboxContainer}, respecting the container's
 * platform perimeter insets.
 */
public class ParaboxLayout {

  private final ParaboxContainer container;
  private final Perimeter perimeter;

  private Box<?> horizontalBox;
  private Box<?> verticalBox;

  /**
   * Creates a layout that uses the perimeter provided by the container's platform.
   *
   * @param container the platform container whose components will be laid out
   */
  public ParaboxLayout (ParaboxContainer container) {

    this(container.getPlatform().getFramePerimeter(), container);
  }

  /**
   * Creates a layout with an explicitly specified perimeter.
   *
   * @param perimeter the insets to reserve around the laid-out area
   * @param container the platform container whose components will be laid out
   */
  public ParaboxLayout (Perimeter perimeter, ParaboxContainer container) {

    this.perimeter = perimeter;
    this.container = container;
  }

  /**
   * Returns the {@link ParaboxContainer} that hosts the components being laid out.
   *
   * @return the owning container
   */
  public ParaboxContainer getContainer () {

    return container;
  }

  /**
   * Returns the box responsible for horizontal sizing and positioning, or {@code null} if not yet set.
   *
   * @return the horizontal box
   */
  public Box<?> getHorizontalBox () {

    return horizontalBox;
  }

  /**
   * Sets the box used for horizontal sizing and positioning of components.
   *
   * @param horizontalBox the box governing horizontal layout
   * @return this layout for method chaining
   */
  public ParaboxLayout setHorizontalBox (Box<?> horizontalBox) {

    this.horizontalBox = horizontalBox;

    return this;
  }

  /**
   * Returns the box responsible for vertical sizing and positioning, or {@code null} if not yet set.
   *
   * @return the vertical box
   */
  public Box<?> getVerticalBox () {

    return verticalBox;
  }

  /**
   * Sets the box used for vertical sizing and positioning of components.
   *
   * @param verticalBox the box governing vertical layout
   * @return this layout for method chaining
   */
  public ParaboxLayout setVerticalBox (Box<?> verticalBox) {

    this.verticalBox = verticalBox;

    return this;
  }

  /**
   * Removes all components from both the horizontal and vertical boxes.
   */
  public void removeAll () {

    horizontalBox.removeAll();
    verticalBox.removeAll();
  }

  /**
   * Removes the specified component from both the horizontal and vertical boxes.
   *
   * @param object the platform component to remove
   */
  public void remove (Object object) {

    horizontalBox.remove(object);
    verticalBox.remove(object);
  }

  /**
   * Calculates the minimum total width, including horizontal perimeter insets.
   *
   * @return the minimum width required by this layout
   * @throws LayoutException if no horizontal box has been set
   */
  public double calculateMinimumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMinimumMeasurement(Bias.HORIZONTAL, null);
  }

  /**
   * Calculates the minimum total height, including vertical perimeter insets.
   *
   * @return the minimum height required by this layout
   * @throws LayoutException if no vertical box has been set
   */
  public double calculateMinimumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMinimumMeasurement(Bias.VERTICAL, null);
  }

  /**
   * Calculates the minimum width and height as a {@link Pair}.
   *
   * @return the minimum width/height pair
   */
  public Pair calculateMinimumSize () {

    return new Pair(calculateMinimumWidth(), calculateMinimumHeight());
  }

  /**
   * Calculates the preferred total width, including horizontal perimeter insets.
   *
   * @return the preferred width for this layout
   * @throws LayoutException if no horizontal box has been set
   */
  public double calculatePreferredWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculatePreferredMeasurement(Bias.HORIZONTAL, null);
  }

  /**
   * Calculates the preferred total height, including vertical perimeter insets.
   *
   * @return the preferred height for this layout
   * @throws LayoutException if no vertical box has been set
   */
  public double calculatePreferredHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculatePreferredMeasurement(Bias.VERTICAL, null);
  }

  /**
   * Calculates the preferred width and height as a {@link Pair}.
   *
   * @return the preferred width/height pair
   */
  public Pair calculatePreferredSize () {

    return new Pair(calculatePreferredWidth(), calculatePreferredHeight());
  }

  /**
   * Calculates the maximum total width, including horizontal perimeter insets.
   *
   * @return the maximum width this layout can occupy
   * @throws LayoutException if no horizontal box has been set
   */
  public double calculateMaximumWidth () {

    if (horizontalBox == null) {
      throw new LayoutException("No horizontal box has been set on this layout");
    }

    return getHorizontalPerimeterRequirements() + horizontalBox.calculateMaximumMeasurement(Bias.HORIZONTAL, null);
  }

  /**
   * Calculates the maximum total height, including vertical perimeter insets.
   *
   * @return the maximum height this layout can occupy
   * @throws LayoutException if no vertical box has been set
   */
  public double calculateMaximumHeight () {

    if (verticalBox == null) {
      throw new LayoutException("No vertical box has been set on this layout");
    }

    return getVerticalPerimeterRequirements() + verticalBox.calculateMaximumMeasurement(Bias.VERTICAL, null);
  }

  /**
   * Calculates the maximum width and height as a {@link Pair}.
   *
   * @return the maximum width/height pair
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
   * Performs the layout pass for the given component array within the specified container dimensions.
   *
   * @param width      the total available width of the container
   * @param height     the total available height of the container
   * @param components the platform components being laid out
   */
  public void doLayout (double width, double height, Object... components) {

    doLayout(width, height, Arrays.asList(components));
  }

  /**
   * Performs the full two-axis layout pass for all components in the list within the specified dimensions,
   * running the horizontal box then the vertical box and verifying all components were placed.
   *
   * @param width         the total available width of the container
   * @param height        the total available height of the container
   * @param componentList the complete list of platform components being laid out
   * @throws LayoutException if the horizontal or vertical box is unset, or if component placement is invalid
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
   * Creates a {@link ParallelBox} with default leading alignment.
   *
   * @return a new parallel box bound to this layout
   */
  public ParallelBox parallelBox () {

    return new ParallelBox(this);
  }

  /**
   * Creates a {@link ParallelBox} with the specified alignment.
   *
   * @param alignment the alignment used to position elements within the box
   * @return a new parallel box bound to this layout
   */
  public ParallelBox parallelBox (Alignment alignment) {

    return new ParallelBox(this, alignment);
  }

  /**
   * Creates a {@link SerialBox} with an unrelated gap and leading justification.
   *
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox () {

    return new SerialBox(this);
  }

  /**
   * Creates a {@link SerialBox} with an unrelated gap, leading justification, and the specified greediness.
   *
   * @param greedy {@code true} to allow the box to consume all remaining space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (boolean greedy) {

    return new SerialBox(this, greedy);
  }

  /**
   * Creates a {@link SerialBox} using the specified predefined gap type.
   *
   * @param gap the gap type to use between elements
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (Gap gap) {

    return new SerialBox(this, gap);
  }

  /**
   * Creates a {@link SerialBox} using the specified predefined gap type and greediness.
   *
   * @param gap    the gap type to use between elements
   * @param greedy {@code true} to allow the box to consume all remaining space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (Gap gap, boolean greedy) {

    return new SerialBox(this, gap, greedy);
  }

  /**
   * Creates a {@link SerialBox} with a fixed numeric gap between elements.
   *
   * @param gap the fixed spacing between consecutive elements
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (double gap) {

    return new SerialBox(this, gap);
  }

  /**
   * Creates a {@link SerialBox} with a fixed numeric gap and the specified greediness.
   *
   * @param gap    the fixed spacing between consecutive elements
   * @param greedy {@code true} to allow the box to consume all remaining space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (double gap, boolean greedy) {

    return new SerialBox(this, gap, greedy);
  }

  /**
   * Creates a {@link SerialBox} with an unrelated gap and the specified justification.
   *
   * @param justification the strategy for distributing surplus space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (Justification justification) {

    return new SerialBox(this, justification);
  }

  /**
   * Creates a {@link SerialBox} with an unrelated gap, the specified justification, and greediness.
   *
   * @param justification the strategy for distributing surplus space
   * @param greedy        {@code true} to allow the box to consume all remaining space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (Justification justification, boolean greedy) {

    return new SerialBox(this, justification, greedy);
  }

  /**
   * Creates a {@link SerialBox} with a predefined gap type and justification.
   *
   * @param gap           the gap type to use between elements
   * @param justification the strategy for distributing surplus space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (Gap gap, Justification justification) {

    return new SerialBox(this, gap, justification);
  }

  /**
   * Creates a {@link SerialBox} with a predefined gap type, justification, and greediness.
   *
   * @param gap           the gap type to use between elements
   * @param justification the strategy for distributing surplus space
   * @param greedy        {@code true} to allow the box to consume all remaining space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (Gap gap, Justification justification, boolean greedy) {

    return new SerialBox(this, gap, justification, greedy);
  }

  /**
   * Creates a {@link SerialBox} with a fixed numeric gap and justification.
   *
   * @param gap           the fixed spacing between consecutive elements
   * @param justification the strategy for distributing surplus space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (double gap, Justification justification) {

    return new SerialBox(this, gap, justification);
  }

  /**
   * Creates a {@link SerialBox} with a fixed numeric gap, justification, and greediness.
   *
   * @param gap           the fixed spacing between consecutive elements
   * @param justification the strategy for distributing surplus space
   * @param greedy        {@code true} to allow the box to consume all remaining space
   * @return a new serial box bound to this layout
   */
  public SerialBox serialBox (double gap, Justification justification, boolean greedy) {

    return new SerialBox(this, gap, justification, greedy);
  }
}
