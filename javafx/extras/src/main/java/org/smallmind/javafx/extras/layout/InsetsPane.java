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
 * A simple wrapper that adds fixed insets around its managed children and reports size hints that include the padding.
 */
public class InsetsPane extends Region {

  private final Insets insets;

  /**
   * Creates a pane that applies the specified insets.
   *
   * @param insets padding to apply around children
   */
  public InsetsPane (Insets insets) {

    this.insets = insets;
  }

  /**
   * Creates a pane with the specified insets and a preconfigured child.
   *
   * @param insets padding to apply around children
   * @param node   the child node to manage
   */
  public InsetsPane (Insets insets, Node node) {

    this(insets);

    getChildren().add(node);
  }

  /**
   * @return the minimum width required to contain the children and padding
   */
  @Override
  protected double computeMinWidth (double v) {

    return computeMeasurement(Cut.MINIMUM, Bias.HORIZONTAL);
  }

  /**
   * @return the minimum height required to contain the children and padding
   */
  @Override
  protected double computeMinHeight (double v) {

    return computeMeasurement(Cut.MINIMUM, Bias.VERTICAL);
  }

  /**
   * @return the preferred width required to contain the children and padding
   */
  @Override
  protected double computePrefWidth (double v) {

    return computeMeasurement(Cut.PREFERRED, Bias.HORIZONTAL);
  }

  /**
   * @return the preferred height required to contain the children and padding
   */
  @Override
  protected double computePrefHeight (double v) {

    return computeMeasurement(Cut.PREFERRED, Bias.VERTICAL);
  }

  /**
   * @return the maximum width required to contain the children and padding
   */
  @Override
  protected double computeMaxWidth (double v) {

    return computeMeasurement(Cut.MAXIMUM, Bias.HORIZONTAL);
  }

  /**
   * @return the maximum height required to contain the children and padding
   */
  @Override
  protected double computeMaxHeight (double v) {

    return computeMeasurement(Cut.MAXIMUM, Bias.VERTICAL);
  }

  /**
   * Computes the requested measurement along the given axis for all managed children and adds the relevant insets.
   *
   * @param cut  which measurement to request (minimum, preferred, maximum)
   * @param bias the axis being measured
   * @return the computed measurement plus padding
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
   * Returns a particular measurement for the supplied child node.
   *
   * @param node the child node
   * @param cut  which measurement to request
   * @param bias the axis being measured
   * @return the node's measurement
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
   * Calculates the padding size along the specified axis.
   *
   * @param bias the axis being measured
   * @return the total inset on that axis
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
   * Positions children inside the padded area.
   */
  @Override
  protected void layoutChildren () {

    for (Node child : getManagedChildren()) {
      child.resizeRelocate(insets.getLeft(), insets.getTop(), getWidth() - insets.getLeft() - insets.getRight(), getHeight() - insets.getTop() - insets.getBottom());
    }
  }
}
