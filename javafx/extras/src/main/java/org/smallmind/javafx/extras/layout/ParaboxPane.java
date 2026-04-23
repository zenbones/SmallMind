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
 * A JavaFX {@link Region} that bridges to the generic parabox layout system. Layout is defined
 * declaratively by assembling {@link SerialBox} and {@link ParallelBox} instances and assigning
 * them as the horizontal and vertical root boxes. The pane delegates all size calculations and
 * child placement to an internal {@link ParaboxLayout} and synchronises scene-graph membership
 * with the layout's component registrations.
 */
public class ParaboxPane extends Region implements ParaboxContainer<Node> {

  private static final ParaboxPlatform PLATFORM = new JavaFxParaboxPlatform();

  private final ParaboxLayout paraboxLayout;

  /**
   * Creates a pane using the default platform frame perimeter (10 px on all sides).
   */
  public ParaboxPane () {

    paraboxLayout = new ParaboxLayout(this);
  }

  /**
   * Creates a pane with the supplied explicit padding instead of the default frame perimeter.
   *
   * @param insets the padding to apply around the layout content; must not be {@code null}
   */
  public ParaboxPane (Insets insets) {

    paraboxLayout = new ParaboxLayout(new Perimeter(insets.getTop(), insets.getLeft(), insets.getBottom(), insets.getRight()), this);
  }

  /**
   * Returns the JavaFX platform adapter that provides default gaps and orientation to the layout
   * engine.
   *
   * @return the platform instance; never {@code null}
   */
  @Override
  public ParaboxPlatform getPlatform () {

    return PLATFORM;
  }

  /**
   * Removes all components from the layout and clears the scene graph accordingly.
   */
  public void removeAll () {

    paraboxLayout.removeAll();
  }

  /**
   * Removes the specified node from the layout and the scene graph, if present.
   *
   * @param node the node to remove; must not be {@code null}
   */
  public void remove (Node node) {

    paraboxLayout.remove(node);
  }

  /**
   * Returns the layout engine's computed minimum width.
   *
   * @param v ignored (height hint)
   * @return minimum width in pixels
   */
  @Override
  protected double computeMinWidth (double v) {

    return paraboxLayout.calculateMinimumWidth();
  }

  /**
   * Returns the layout engine's computed minimum height.
   *
   * @param v ignored (width hint)
   * @return minimum height in pixels
   */
  @Override
  protected double computeMinHeight (double v) {

    return paraboxLayout.calculateMinimumHeight();
  }

  /**
   * Returns the layout engine's computed preferred width.
   *
   * @param v ignored (height hint)
   * @return preferred width in pixels
   */
  @Override
  protected double computePrefWidth (double v) {

    return paraboxLayout.calculatePreferredWidth();
  }

  /**
   * Returns the layout engine's computed preferred height.
   *
   * @param v ignored (width hint)
   * @return preferred height in pixels
   */
  @Override
  protected double computePrefHeight (double v) {

    return paraboxLayout.calculatePreferredHeight();
  }

  /**
   * Returns {@link Double#MAX_VALUE}, allowing the pane to grow without bound in width.
   *
   * @param v ignored
   * @return {@link Double#MAX_VALUE}
   */
  @Override
  protected double computeMaxWidth (double v) {

    return Double.MAX_VALUE;
  }

  /**
   * Returns {@link Double#MAX_VALUE}, allowing the pane to grow without bound in height.
   *
   * @param v ignored
   * @return {@link Double#MAX_VALUE}
   */
  @Override
  protected double computeMaxHeight (double v) {

    return Double.MAX_VALUE;
  }

  /**
   * Executes the parabox layout pass, positioning and sizing all registered child nodes within
   * the pane's current bounds.
   */
  @Override
  protected void layoutChildren () {

    paraboxLayout.doLayout(getWidth(), getHeight(), getChildrenUnmodifiable());
  }

  /**
   * Wraps {@code node} in a {@link JavaFxParaboxElement} so it can be managed by the layout engine.
   *
   * @param node       the node to wrap; must not be {@code null}
   * @param constraint the sizing and positioning constraint to apply; must not be {@code null}
   * @return a new parabox element backed by the node
   */
  @Override
  public ParaboxElement<Node> constructElement (Node node, Constraint constraint) {

    return new JavaFxParaboxElement(node, constraint);
  }

  /**
   * Adds {@code node} to the JavaFX scene graph as a child of this pane if it is not already
   * present.
   *
   * @param node the node to add; must not be {@code null}
   */
  @Override
  public void nativelyAddComponent (Node node) {

    List<Node> children;

    if (!(children = getChildren()).contains(node)) {
      children.add(node);
    }
  }

  /**
   * Removes {@code component} from the JavaFX scene graph.
   *
   * @param component the node to remove; must not be {@code null}
   */
  @Override
  public void nativelyRemoveComponent (Node component) {

    getChildren().remove(component);
  }

  /**
   * Returns the root horizontal box that drives horizontal layout calculations.
   *
   * @return the horizontal box, or {@code null} if none has been set
   */
  public Box getHorizontalBox () {

    return paraboxLayout.getHorizontalBox();
  }

  /**
   * Sets the root horizontal box and triggers a layout update.
   *
   * @param horizontalBox the horizontal layout definition; must not be {@code null}
   * @return this pane for method chaining
   */
  public ParaboxPane setHorizontalBox (Box horizontalBox) {

    paraboxLayout.setHorizontalBox(horizontalBox);

    return this;
  }

  /**
   * Returns the root vertical box that drives vertical layout calculations.
   *
   * @return the vertical box, or {@code null} if none has been set
   */
  public Box getVerticalBox () {

    return paraboxLayout.getVerticalBox();
  }

  /**
   * Sets the root vertical box and triggers a layout update.
   *
   * @param verticalBox the vertical layout definition; must not be {@code null}
   * @return this pane for method chaining
   */
  public ParaboxPane setVerticalBox (Box verticalBox) {

    paraboxLayout.setVerticalBox(verticalBox);

    return this;
  }

  /**
   * Creates a {@link ParallelBox} with the default alignment.
   *
   * @return a new parallel box
   */
  public ParallelBox parallelBox () {

    return paraboxLayout.parallelBox();
  }

  /**
   * Creates a {@link ParallelBox} with the given alignment applied to all contained elements.
   *
   * @param alignment the alignment strategy; must not be {@code null}
   * @return a new parallel box
   */
  public ParallelBox parallelBox (Alignment alignment) {

    return paraboxLayout.parallelBox(alignment);
  }

  /**
   * Creates a {@link SerialBox} with the default gap and justification.
   *
   * @return a new serial box
   */
  public SerialBox serialBox () {

    return paraboxLayout.serialBox();
  }

  /**
   * Creates a {@link SerialBox} with the default gap and the given greediness hint.
   *
   * @param greedy {@code true} if the box should expand to consume available space
   * @return a new serial box
   */
  public SerialBox serialBox (boolean greedy) {

    return paraboxLayout.serialBox(greedy);
  }

  /**
   * Creates a {@link SerialBox} with the given named gap constant.
   *
   * @param gap the gap between child elements; must not be {@code null}
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap) {

    return paraboxLayout.serialBox(gap);
  }

  /**
   * Creates a {@link SerialBox} with the given named gap and greediness hint.
   *
   * @param gap    the gap between child elements; must not be {@code null}
   * @param greedy {@code true} if the box should expand to consume available space
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  /**
   * Creates a {@link SerialBox} with the given numeric pixel gap.
   *
   * @param gap gap in pixels between child elements
   * @return a new serial box
   */
  public SerialBox serialBox (double gap) {

    return paraboxLayout.serialBox(gap);
  }

  /**
   * Creates a {@link SerialBox} with the given numeric pixel gap and greediness hint.
   *
   * @param gap    gap in pixels between child elements
   * @param greedy {@code true} if the box should expand to consume available space
   * @return a new serial box
   */
  public SerialBox serialBox (double gap, boolean greedy) {

    return paraboxLayout.serialBox(gap, greedy);
  }

  /**
   * Creates a {@link SerialBox} with the given justification strategy.
   *
   * @param justification how remaining space is distributed among child elements; must not be {@code null}
   * @return a new serial box
   */
  public SerialBox serialBox (Justification justification) {

    return paraboxLayout.serialBox(justification);
  }

  /**
   * Creates a {@link SerialBox} with the given justification and greediness hint.
   *
   * @param justification how remaining space is distributed; must not be {@code null}
   * @param greedy        {@code true} if the box should expand to consume available space
   * @return a new serial box
   */
  public SerialBox serialBox (Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(justification, greedy);
  }

  /**
   * Creates a {@link SerialBox} with the given named gap and justification.
   *
   * @param gap           the gap between child elements; must not be {@code null}
   * @param justification how remaining space is distributed; must not be {@code null}
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  /**
   * Creates a {@link SerialBox} with the given named gap, justification, and greediness hint.
   *
   * @param gap           the gap between child elements; must not be {@code null}
   * @param justification how remaining space is distributed; must not be {@code null}
   * @param greedy        {@code true} if the box should expand to consume available space
   * @return a new serial box
   */
  public SerialBox serialBox (Gap gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }

  /**
   * Creates a {@link SerialBox} with the given numeric pixel gap and justification.
   *
   * @param gap           gap in pixels between child elements
   * @param justification how remaining space is distributed; must not be {@code null}
   * @return a new serial box
   */
  public SerialBox serialBox (double gap, Justification justification) {

    return paraboxLayout.serialBox(gap, justification);
  }

  /**
   * Creates a {@link SerialBox} with the given numeric pixel gap, justification, and greediness hint.
   *
   * @param gap           gap in pixels between child elements
   * @param justification how remaining space is distributed; must not be {@code null}
   * @param greedy        {@code true} if the box should expand to consume available space
   * @return a new serial box
   */
  public SerialBox serialBox (double gap, Justification justification, boolean greedy) {

    return paraboxLayout.serialBox(gap, justification, greedy);
  }
}
