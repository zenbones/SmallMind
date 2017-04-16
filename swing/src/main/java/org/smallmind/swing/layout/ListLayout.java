/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.io.Serializable;

public class ListLayout implements LayoutManager, Serializable {

  private int gap;

  public ListLayout () {

    this(0);
  }

  public ListLayout (int gap) {

    this.gap = gap;
  }

  public void addLayoutComponent (String s, Component component) {

  }

  public void removeLayoutComponent (Component component) {

  }

  public Dimension preferredLayoutSize (Container container) {

    Component component;
    Dimension preferredSize;
    int height = 0;
    int width;

    width = container.getWidth() - container.getInsets().left - container.getInsets().right;

    synchronized (container.getTreeLock()) {
      for (int count = 0; count < container.getComponentCount(); count++) {
        component = container.getComponent(count);
        if (component.isVisible()) {
          preferredSize = component.getPreferredSize();
          height += preferredSize.getHeight();
          if (count > 0) {
            height += gap;
          }
        }
      }

      height += container.getInsets().top + container.getInsets().bottom;

      return new Dimension(width, height);
    }
  }

  public Dimension minimumLayoutSize (Container container) {

    Component component;
    Dimension minSize;
    int height = 0;
    int width;

    width = container.getWidth() - container.getInsets().left - container.getInsets().right;

    synchronized (container.getTreeLock()) {
      for (int count = 0; count < container.getComponentCount(); count++) {
        component = container.getComponent(count);
        if (component.isVisible()) {
          minSize = component.getMinimumSize();
          height += minSize.getHeight();
          if (count > 0) {
            height += gap;
          }
        }
      }

      height += container.getInsets().top + container.getInsets().bottom;

      return new Dimension(width, height);
    }
  }

  public void layoutContainer (Container container) {

    Component component;
    Dimension preferredSize;
    int width;
    int xPos;
    int yPos;

    width = container.getWidth() - container.getInsets().left - container.getInsets().right;
    xPos = container.getInsets().left;
    yPos = container.getInsets().top;

    synchronized (container.getTreeLock()) {
      for (int count = 0; count < container.getComponentCount(); count++) {
        component = container.getComponent(count);
        if (component.isVisible()) {
          preferredSize = component.getPreferredSize();
          component.setLocation(xPos, yPos);
          component.setSize(width, (int)preferredSize.getHeight());
          yPos += (preferredSize.getHeight() + gap);
        }
      }
    }
  }

}
