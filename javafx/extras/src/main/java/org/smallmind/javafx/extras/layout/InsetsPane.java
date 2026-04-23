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

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.layout.Bias;

/**
 * A {@link Region} that wraps its managed children with fixed padding. All size hints reported
 * to the layout system include the padding on the relevant axis. During layout each child is
 * relocated and resized to fill the area remaining after the insets are applied.
 */
public class InsetsPane extends Region {

  private final Insets insets;

  /**
   * Creates a pane that applies the given insets around its children.
   *
   * @param insets the padding to apply; must not be {@code null}
   */
  public InsetsPane (Insets insets) {

    this.insets = insets;
  }

  /**
   * Creates a pane that applies the given insets and immediately adds {@code node} as a managed child.
   *
   * @param insets the padding to apply; must not be {@code null}
   * @param node   the initial child to add; must not be {@code null}
   */
  public InsetsPane (Insets insets, Node node) {

    this(insets);

    getChildren().add(node);
  }

  /**
   * Returns the minimum width of the pane: the largest minimum width among all managed children
   * plus the horizontal insets.
   *
   * @param v ignored (height hint)
   * @return minimum width in pixels
   */
  @Override
  protected double computeMinWidth (double v) {

    return computeMeasurement(Cut.MINIMUM, Bias.HORIZONTAL);
  }

  /**
   * Returns the minimum height of the pane: the largest minimum height among all managed children
   * plus the vertical insets.
   *
   * @param v ignored (width hint)
   * @return minimum height in pixels
   */
  @Override
  protected double computeMinHeight (double v) {

    return computeMeasurement(Cut.MINIMUM, Bias.VERTICAL);
  }

  /**
   * Returns the preferred width of the pane: the largest preferred width among all managed children
   * plus the horizontal insets.
   *
   * @param v ignored (height hint)
   * @return preferred width in pixels
   */
  @Override
  protected double computePrefWidth (double v) {

    return computeMeasurement(Cut.PREFERRED, Bias.HORIZONTAL);
  }

  /**
   * Returns the preferred height of the pane: the largest preferred height among all managed
   * children plus the vertical insets.
   *
   * @param v ignored (width hint)
   * @return preferred height in pixels
   */
  @Override
  protected double computePrefHeight (double v) {

    return computeMeasurement(Cut.PREFERRED, Bias.VERTICAL);
  }

  /**
   * Returns the maximum width of the pane: the largest maximum width among all managed children
   * plus the horizontal insets.
   *
   * @param v ignored (height hint)
   * @return maximum width in pixels
   */
  @Override
  protected double computeMaxWidth (double v) {

    return computeMeasurement(Cut.MAXIMUM, Bias.HORIZONTAL);
  }

  /**
   * Returns the maximum height of the pane: the largest maximum height among all managed children
   * plus the vertical insets.
   *
   * @param v ignored (width hint)
   * @return maximum height in pixels
   */
  @Override
  protected double computeMaxHeight (double v) {

    return computeMeasurement(Cut.MAXIMUM, Bias.VERTICAL);
  }

  /**
   * Computes a size measurement along the given axis for all managed children and adds the
   * relevant padding.
   *
   * @param cut  which measurement variant to request ({@link Cut#MINIMUM}, {@link Cut#PREFERRED},
   *             or {@link Cut#MAXIMUM})
   * @param bias the axis being measured
   * @return the computed size including padding, in pixels
   */
  private double computeMeasurement (Cut cut, Bias bias) {

    double measurement = 0;

    for (Node child : getManagedChildren()) {

      double childMeasurement;

      if ((childMeasurement = getChildMeasurement(child, cut, bias)) > measurement) {
        measurement = childMeasurement;
      }
    }

    return measurement + getGutter(bias);
  }

  /**
   * Returns the requested measurement for a single child node along the given axis.
   *
   * @param node the child node to measure
   * @param cut  which measurement variant to request
   * @param bias the axis being measured
   * @return the requested size in pixels
   * @throws org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException if an unexpected {@link Cut}
   *                                                                  or {@link Bias} value is encountered
   */
  private double getChildMeasurement (Node node, Cut cut, Bias bias) {

    switch (cut) {
      case MINIMUM:
        switch (bias) {
          case HORIZONTAL:
            return node.minWidth(-1);
          case VERTICAL:
            return node.minHeight(-1);
          default:
            throw new UnknownSwitchCaseException(bias.name());
        }
      case PREFERRED:
        switch (bias) {
          case HORIZONTAL:
            return node.prefWidth(-1);
          case VERTICAL:
            return node.prefHeight(-1);
          default:
            throw new UnknownSwitchCaseException(bias.name());
        }
      case MAXIMUM:
        switch (bias) {
          case HORIZONTAL:
            return node.maxWidth(-1);
          case VERTICAL:
            return node.maxHeight(-1);
          default:
            throw new UnknownSwitchCaseException(bias.name());
        }
      default:
        throw new UnknownSwitchCaseException(cut.name());
    }
  }

  /**
   * Returns the total padding amount along the given axis.
   *
   * @param bias the axis for which to sum insets
   * @return left + right insets for {@link Bias#HORIZONTAL}, or top + bottom for {@link Bias#VERTICAL}
   * @throws org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException if an unexpected {@link Bias}
   *                                                                  value is encountered
   */
  private double getGutter (Bias bias) {

    switch (bias) {
      case HORIZONTAL:
        return insets.getLeft() + insets.getRight();
      case VERTICAL:
        return insets.getTop() + insets.getBottom();
      default:
        throw new UnknownSwitchCaseException(bias.name());
    }
  }

  /**
   * Resizes and relocates each managed child to fill the area within the insets.
   */
  @Override
  protected void layoutChildren () {

    for (Node child : getManagedChildren()) {
      child.resizeRelocate(insets.getLeft(), insets.getTop(), getWidth() - insets.getLeft() - insets.getRight(), getHeight() - insets.getTop() - insets.getBottom());
    }
  }
}
