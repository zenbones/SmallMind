/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.swing.layout;

import java.awt.Component;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.ComponentParaboxElement;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Pair;

public class SwingParaboxElement extends ComponentParaboxElement<Component> {

  public SwingParaboxElement (Component component, Constraint constraint) {

    super(component, constraint);
  }

  @Override
  public double getComponentMinimumMeasurement (Bias bias) {

    return bias.equals(Bias.HORIZONTAL) ? getPart().getMinimumSize().getWidth() : getPart().getMinimumSize().getHeight();
  }

  @Override
  public double getComponentPreferredMeasurement (Bias bias) {

    return bias.equals(Bias.HORIZONTAL) ? getPart().getPreferredSize().getWidth() : getPart().getPreferredSize().getHeight();
  }

  @Override
  public double getComponentMaximumMeasurement (Bias bias) {

    return bias.equals(Bias.HORIZONTAL) ? getPart().getMaximumSize().getWidth() : getPart().getMaximumSize().getHeight();
  }

  public double getBaseline (Bias bias, double measurement) {

    int swingBaseline;

    return ((swingBaseline = getPart().getBaseline((int)(bias.equals(Bias.HORIZONTAL) ? measurement : 0), (int)(bias.equals(Bias.VERTICAL) ? measurement : 0))) < 0) ? measurement : swingBaseline;
  }

  @Override
  public void applyLayout (Pair position, Pair size) {

    getPart().setBounds((int)position.getFirst(), (int)position.getSecond(), (int)size.getFirst(), (int)size.getSecond());
  }
}
