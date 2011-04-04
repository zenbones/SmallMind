/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.swing;

import java.awt.Dimension;
import javax.swing.JComponent;

public class ComponentUtilities {

  public static void setWidth (JComponent component, int width) {

    component.setSize(new Dimension(width, component.getHeight()));
  }

  public static void setHeight (JComponent component, int height) {

    component.setSize(new Dimension(component.getWidth(), height));
  }

  public static int getPreferredWidth (JComponent component) {

    return (int)component.getPreferredSize().getWidth();
  }

  public static void setPreferredWidth (JComponent component, int width) {

    component.setPreferredSize(new Dimension(width, (int)component.getPreferredSize().getHeight()));
  }

  public static int getMinimumWidth (JComponent component) {

    return (int)component.getMinimumSize().getWidth();
  }

  public static void setMinimumWidth (JComponent component, int width) {

    component.setMinimumSize(new Dimension(width, (int)component.getMinimumSize().getHeight()));
  }

  public static int getMaximumWidth (JComponent component) {

    return (int)component.getMaximumSize().getWidth();
  }

  public static void setMaximumWidth (JComponent component, int width) {

    component.setMaximumSize(new Dimension(width, (int)component.getMaximumSize().getHeight()));
  }

  public static int getPreferredHeight (JComponent component) {

    return (int)component.getPreferredSize().getHeight();
  }

  public static void setPreferredHeight (JComponent component, int height) {

    component.setPreferredSize(new Dimension((int)component.getPreferredSize().getWidth(), height));
  }

  public static int getMinimumHeight (JComponent component) {

    return (int)component.getMinimumSize().getHeight();
  }

  public static void setMinimumHeight (JComponent component, int height) {

    component.setMinimumSize(new Dimension((int)component.getMinimumSize().getWidth(), height));
  }

  public static int getMaximumHeight (JComponent component) {

    return (int)component.getMaximumSize().getHeight();
  }

  public static void setMaximumHeight (JComponent component, int height) {

    component.setMaximumSize(new Dimension((int)component.getMaximumSize().getWidth(), height));
  }

}
