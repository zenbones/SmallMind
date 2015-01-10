/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

public class InsetsPane extends Region {

  private Insets insets;

  public InsetsPane (Insets insets) {

    this.insets = insets;
  }

  public InsetsPane (Insets insets, Node node) {

    this(insets);

    getChildren().add(node);
  }

  @Override
  protected double computeMinWidth (double v) {

    return computeMeasurement(Cut.MINIMUM, Bias.HORIZONTAL);
  }

  @Override
  protected double computeMinHeight (double v) {

    return computeMeasurement(Cut.MINIMUM, Bias.VERTICAL);
  }

  @Override
  protected double computePrefWidth (double v) {

    return computeMeasurement(Cut.PREFERRED, Bias.HORIZONTAL);
  }

  @Override
  protected double computePrefHeight (double v) {

    return computeMeasurement(Cut.PREFERRED, Bias.VERTICAL);
  }

  @Override
  protected double computeMaxWidth (double v) {

    return computeMeasurement(Cut.MAXIMUM, Bias.HORIZONTAL);
  }

  @Override
  protected double computeMaxHeight (double v) {

    return computeMeasurement(Cut.MAXIMUM, Bias.VERTICAL);
  }

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

  @Override
  protected void layoutChildren () {

    for (Node child : getManagedChildren()) {
      child.resizeRelocate(insets.getLeft(), insets.getTop(), getWidth() - insets.getLeft() - insets.getRight(), getHeight() - insets.getTop() - insets.getBottom());
    }
  }
}
