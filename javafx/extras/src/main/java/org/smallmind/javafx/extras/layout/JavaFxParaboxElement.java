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

import javafx.scene.Node;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.ComponentParaboxElement;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Pair;
import org.smallmind.nutsnbolts.layout.ParaboxLayout;

/**
 * {@link ComponentParaboxElement} implementation for JavaFX {@link Node} instances, supplying measurement and layout
 * information to the parabox layout engine.
 */
public class JavaFxParaboxElement extends ComponentParaboxElement<Node> {

  /**
   * Wraps a JavaFX node with the specified constraint for use in a {@link ParaboxLayout}.
   *
   * @param node       the JavaFX node to wrap
   * @param constraint the constraint describing how the node should be sized and positioned
   */
  public JavaFxParaboxElement (Node node, Constraint constraint) {

    super(node, constraint);
  }

  /**
   * Returns the minimum size along the given axis by querying the underlying node.
   *
   * @param bias the axis being measured
   * @return the minimum size reported by the node
   */
  @Override
  public double getComponentMinimumMeasurement (Bias bias) {

    return switch (bias) {
      case HORIZONTAL -> getPart().minWidth(-1);
      case VERTICAL -> getPart().minHeight(-1);
    };
  }

  /**
   * Returns the preferred size along the given axis by querying the underlying node.
   *
   * @param bias the axis being measured
   * @return the preferred size reported by the node
   */
  @Override
  public double getComponentPreferredMeasurement (Bias bias) {

    return switch (bias) {
      case HORIZONTAL -> getPart().prefWidth(-1);
      case VERTICAL -> getPart().prefHeight(-1);
    };
  }

  /**
   * Returns the maximum size along the given axis by querying the underlying node.
   *
   * @param bias the axis being measured
   * @return the maximum size reported by the node
   */
  @Override
  public double getComponentMaximumMeasurement (Bias bias) {

    return switch (bias) {
      case HORIZONTAL -> getPart().maxWidth(-1);
      case VERTICAL -> getPart().maxHeight(-1);
    };
  }

  /**
   * Calculates the baseline for the node based on the supplied axis and measurement.
   *
   * @param bias        the axis being measured
   * @param measurement the measurement supplied by the layout
   * @return the baseline offset for vertical bias, or preferred height for horizontal bias
   */
  @Override
  public double getBaseline (Bias bias, double measurement) {

    return bias.equals(Bias.VERTICAL) ? getPart().getBaselineOffset() : getPart().prefHeight(-1);
  }

  /**
   * Applies the computed location and size to the underlying node.
   *
   * @param location the top-left coordinate
   * @param size     the width and height to apply
   */
  @Override
  public void applyLayout (Pair location, Pair size) {

    getPart().resizeRelocate(location.getFirst(), location.getSecond(), size.getFirst(), size.getSecond());
  }
}
