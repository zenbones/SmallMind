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
package org.smallmind.javafx.layout;

import javafx.scene.Node;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.ComponentParaboxElement;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Pair;

public class JavaFxParaboxElement extends ComponentParaboxElement<Node> {

  public JavaFxParaboxElement (Node node, Constraint constraint) {

    super(node, constraint);
  }

  @Override
  public double getComponentMinimumMeasurement (Bias bias) {

    switch (bias) {
      case HORIZONTAL:
        return getPart().minWidth(-1);
      case VERTICAL:
        return getPart().minHeight(-1);
      default:
        throw new UnknownSwitchCaseException(bias.name());
    }
  }

  @Override
  public double getComponentPreferredMeasurement (Bias bias) {

    switch (bias) {
      case HORIZONTAL:
        return getPart().prefWidth(-1);
      case VERTICAL:
        return getPart().prefHeight(-1);
      default:
        throw new UnknownSwitchCaseException(bias.name());
    }
  }

  @Override
  public double getComponentMaximumMeasurement (Bias bias) {

    switch (bias) {
      case HORIZONTAL:
        return getPart().maxWidth(-1);
      case VERTICAL:
        return getPart().maxHeight(-1);
      default:
        throw new UnknownSwitchCaseException(bias.name());
    }
  }

  @Override
  public double getBaseline (Bias bias, double measurement) {

    return bias.equals(Bias.VERTICAL) ? getPart().getBaselineOffset() : getPart().prefHeight(-1);
  }

  @Override
  public void applyLayout (Pair location, Pair size) {

    getPart().resizeRelocate(location.getFirst(), location.getSecond(), size.getFirst(), size.getSecond());
  }
}
