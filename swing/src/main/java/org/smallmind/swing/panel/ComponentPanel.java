/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.swing.panel;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

public class ComponentPanel extends JPanel implements Scrollable {

  public ComponentPanel (LayoutManager layoutManager) {

    super(layoutManager);
  }

  public synchronized int getIndexAtPoint (Point point) {

    int height = 0;

    for (int count = 0; count < getComponentCount(); count++) {
      height += getComponent(count).getPreferredSize().getHeight();
      if (point.getY() <= height) {
        return count;
      }
    }

    return -1;
  }

  public synchronized Rectangle getSquashedRectangleAtIndex (int index) {

    int yPos = 0;

    for (int count = 0; count < index; count++) {
      yPos += getComponent(count).getPreferredSize().getHeight();
    }

    return new Rectangle(0, yPos, 0, (int)getComponent(index).getPreferredSize().getHeight());
  }

  public Dimension getPreferredScrollableViewportSize () {

    return getPreferredSize();
  }

  public boolean getScrollableTracksViewportWidth () {

    return true;
  }

  public boolean getScrollableTracksViewportHeight () {

    return false;
  }

  public int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {

    Rectangle viewRectangle;
    int index;
    int jiggleJump = 0;

    viewRectangle = ((JViewport)getParent()).getViewRect();

    if (direction < 0) {
      if (viewRectangle.getY() > 0) {
        index = getIndexAtPoint(new Point(0, (int)viewRectangle.getY()));
        jiggleJump = (int)(viewRectangle.getY() - getSquashedRectangleAtIndex(index).getY());
        if (jiggleJump == 0) {
          jiggleJump += getSquashedRectangleAtIndex(index - 1).getHeight();
        }
      }
    }
    else if (direction > 0) {
      if ((viewRectangle.getY() + viewRectangle.getHeight()) < getPreferredSize().getHeight()) {
        index = getIndexAtPoint(new Point(0, (int)(viewRectangle.getY() + viewRectangle.getHeight())));
        jiggleJump = (int)((getSquashedRectangleAtIndex(index).getY() + getSquashedRectangleAtIndex(index).getHeight()) - (viewRectangle.getY() + viewRectangle.getHeight()));
        if (jiggleJump == 0) {
          if (index >= (getComponentCount() - 1)) {
            jiggleJump = 0;
          }
          else {
            jiggleJump += getSquashedRectangleAtIndex(index + 1).getHeight();
          }
        }
      }
    }

    return jiggleJump;
  }

  public int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction) {

    Dimension preferredSize;
    Rectangle viewRectangle;

    viewRectangle = ((JViewport)getParent()).getViewRect();

    if (direction < 0) {
      return (int)viewRectangle.getY();
    }
    else if (direction > 0) {
      preferredSize = getPreferredSize();

      return (int)(preferredSize.getHeight() - viewRectangle.getY() + viewRectangle.getHeight());
    }

    return 0;
  }

}
