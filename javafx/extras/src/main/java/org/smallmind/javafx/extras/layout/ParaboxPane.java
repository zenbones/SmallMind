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
package org.smallmind.javafx.extras.layout;

import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Box;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.Justification;
import org.smallmind.nutsnbolts.layout.ParaboxContainer;
import org.smallmind.nutsnbolts.layout.ParaboxElement;
import org.smallmind.nutsnbolts.layout.ParaboxLayout;
import org.smallmind.nutsnbolts.layout.ParaboxPlatform;
import org.smallmind.nutsnbolts.layout.ParallelBox;
import org.smallmind.nutsnbolts.layout.Perimeter;
import org.smallmind.nutsnbolts.layout.SerialBox;

/**
 * JavaFX {@link Region} that bridges to the generic {@link ParaboxLayout} system, allowing declarative layout
 * definitions using serial and parallel boxes.
 */
public class ParaboxPane extends Region implements ParaboxContainer<Node> {

  private static final ParaboxPlatform PLATFORM = new JavaFxParaboxPlatform();

  private final ParaboxLayout paraboxLayout;

  /**
   * Constructs a pane with default platform perimeter insets.
   */
  public ParaboxPane () {

    paraboxLayout = new ParaboxLayout(this);
  }

  /**
   * Constructs a pane with explicit insets surrounding its content.
   *
   * @param insets the padding to apply around the layout
   */
  public ParaboxPane (Insets insets) {

    paraboxLayout = new ParaboxLayout(new Perimeter(insets.getTop(), insets.getLeft(), insets.getBottom(), insets.getRight()), this);
  }

  /**
   * @return the JavaFX-specific platform adapter
   */
  @Override
  public ParaboxPlatform getPlatform () {

    return PLATFORM;
  }

  /**
   * Removes all components managed by the layout.
   */
  public void removeAll () {

    paraboxLayout.removeAll();
  }

  /**
   * Removes the supplied node from the layout if present.
   *
   * @param node the component to remove
   */
  public void remove (Node node) {

    paraboxLayout.remove(node);
  }

  /**
   * @return the minimum width calculated by the parabox layout
   */
  @Override
  protected double computeMinWidth (double v) {

    return paraboxLayout.calculateMinimumWidth();
  }

  /**
   * @return the minimum height calculated by the parabox layout
   */
  @Override
  protected double computeMinHeight (double v) {

    return paraboxLayout.calculateMinimumHeight();
  }

  /**
   * @return the preferred width calculated by the parabox layout
   */
  @Override
  protected double computePrefWidth (double v) {

    return paraboxLayout.calculatePreferredWidth();
  }

  /**
   * @return the preferred height calculated by the parabox layout
   */
  @Override
  protected double computePrefHeight (double v) {

    return paraboxLayout.calculatePreferredHeight();
  }

  /**
   * @return an unconstrained maximum width value
   */
  @Override
  protected double computeMaxWidth (double v) {

    return Double.MAX_VALUE;
  }

  /**
   * @return an unconstrained maximum height value
   */
  @Override
  protected double computeMaxHeight (double v) {

    return Double.MAX_VALUE;
  }

  /**
   * Applies the parabox layout to child nodes using the current bounds.
   */
  @Override
  protected void layoutChildren () {

    paraboxLayout.doLayout(getWidth(), getHeight(), getChildrenUnmodifiable());
  }

  /**
   * Wraps a JavaFX {@link Node} in a parabox element so it can participate in the layout.
   *
   * @param node       the node to wrap
   * @param constraint the layout constraint applied to the node
   * @return a parabox element backed by the provided node
   */
  @Override
  public ParaboxElement<Node> constructElement (Node node, Constraint constraint) {

    return new JavaFxParaboxElement(node, constraint);
  }

  /**
   * Adds the component to the JavaFX scene graph if not already present.
   *
   * @param node the node to add
   */
  @Override
  public void nativelyAddComponent (Node node) {

    List<Node> children;

    if (!(children = getChildren()).contains(node)) {
      children.add(node);
    }
  }

  /**
   * Removes the component from the JavaFX scene graph.
   *
   * @param component the node to remove
   */
  @Override
  public void nativelyRemoveComponent (Node component) {

    getChildren().remove(component);
  }

  /**
   * @return the root horizontal box driving layout calculations
   */
  public Box getHorizontalBox () {

    return paraboxLayout.getHorizontalBox();
  }

  /**
   * Sets the root horizontal box driving layout calculations.
   *
   * @param horizontalBox the horizontal box definition
   * @return this pane for chaining
   */
  public ParaboxPane setHorizontalBox (Box horizontalBox) {

    paraboxLayout.setHorizontalBox(horizontalBox);

    return this;
  }

  /**
   * @return the root vertical box driving layout calculations
   */
  public Box getVerticalBox () {

    return paraboxLayout.getVerticalBox();
  }

  /**
   * Sets the root vertical box driving layout calculations.
   *
   * @param verticalBox the vertical box definition
   * @return this pane for chaining
   */
  public ParaboxPane setVerticalBox (Box verticalBox) {

    paraboxLayout.setVerticalBox(verticalBox);

    return this;
  }

  /**
   * Convenience creation of a parallel box using default alignment.
   *
   * @return the created parallel box
   */
  public ParallelBox parallelBox () {

    return paraboxLayout.parallelBox();
  }

  /**
   * Convenience creation of a parallel box using the provided alignment.
   *
   * @param alignment alignment applied to contained elements
   * @return the created parallel box
   */
  public ParallelBox parallelBox (Alignment alignment) {

    return paraboxLayout.parallelBox(alignment);
  }

  /**
   * Convenience creation of a serial box with default spacing.
   *
   * @return the created serial box
   */
  public SerialBox serialBox () {

    return paraboxLayout.serialBox();
  }

  /**
   * Creates a serial box optionally marked greedy for expansion.
   *
   * @param greedy whether the box should consume extra space
   * @return the created serial box
   */
  public SerialBox serialBox (boolean greedy) {

    return paraboxLayout.serialBox(greedy);
  }

  /**
   * Creates a serial box with the supplied gap.
   *
   * @param gap gap between children
   * @return the created serial box
   */
  public SerialBox serialBox (Gap gap) {

    return paraboxLayout.serialBox(gap);
  }

  /**
   * Creates a serial box with the supplied gap and greediness.
   *
   * @param gap    gap between children
   * @param greedy whether the box should consume extra space
   * @return the created serial box
   */
  public SerialBox serialBox (Gap gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  /**
   * Creates a serial box with the supplied gap size.
   *
   * @param gap gap size between children
   * @return the created serial box
   */
  public SerialBox serialBox (double gap) {

    return paraboxLayout.serialBox(gap);
  }

  /**
   * Creates a serial box with the supplied gap size and greediness.
   *
   * @param gap    gap size between children
   * @param greedy whether the box should consume extra space
   * @return the created serial box
   */
  public SerialBox serialBox (double gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  /**
   * Creates a serial box using the supplied justification.
   *
   * @param justification justification strategy
   * @return the created serial box
   */
  public SerialBox serialBox (Justification justification) {

    return paraboxLayout.serialBox(justification);
  }

  /**
   * Creates a serial box with justification and greediness hints.
   *
   * @param justification justification strategy
   * @param greedy        whether the box should consume extra space
   * @return the created serial box
   */
  public SerialBox serialBox (Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(justification, greedy);
  }

  /**
   * Creates a serial box with a {@link Gap} and justification.
   *
   * @param gap           gap between children
   * @param justification justification strategy
   * @return the created serial box
   */
  public SerialBox serialBox (Gap gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  /**
   * Creates a serial box with a {@link Gap}, justification, and greediness hint.
   *
   * @param gap           gap between children
   * @param justification justification strategy
   * @param greedy        whether the box should consume extra space
   * @return the created serial box
   */
  public SerialBox serialBox (Gap gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }

  /**
   * Creates a serial box with a numeric gap and justification.
   *
   * @param gap           numeric gap size
   * @param justification justification strategy
   * @return the created serial box
   */
  public SerialBox serialBox (double gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  /**
   * Creates a serial box with a numeric gap, justification, and greediness hint.
   *
   * @param gap           numeric gap size
   * @param justification justification strategy
   * @param greedy        whether the box should consume extra space
   * @return the created serial box
   */
  public SerialBox serialBox (double gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }
}
