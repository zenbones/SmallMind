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
package org.smallmind.swing.catalog;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JViewport;

public class DefaultCatalogScrollModel implements CatalogScrollModel {

  private Catalog catalog;

  public DefaultCatalogScrollModel (Catalog catalog) {

    this.catalog = catalog;
  }

  public int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {

    Rectangle viewRectangle;
    int index;
    int jiggleJump = 0;

    viewRectangle = ((JViewport)catalog.getParent()).getViewRect();

    if (direction < 0) {
      if (viewRectangle.getY() > 0) {
        index = catalog.getIndexAtPoint(new Point(0, (int)viewRectangle.getY()));
        jiggleJump = (int)(viewRectangle.getY() - catalog.getSquashedRectangleAtIndex(index).getY());
        if (jiggleJump == 0) {
          jiggleJump += catalog.getSquashedRectangleAtIndex(index - 1).getHeight();
        }
      }
    }
    else if (direction > 0) {
      if ((viewRectangle.getY() + viewRectangle.getHeight()) < catalog.getPreferredSize().getHeight()) {
        index = catalog.getIndexAtPoint(new Point(0, (int)(viewRectangle.getY() + viewRectangle.getHeight())));
        jiggleJump = (int)((catalog.getSquashedRectangleAtIndex(index).getY() + catalog.getSquashedRectangleAtIndex(index).getHeight()) - (viewRectangle.getY() + viewRectangle.getHeight()));
        if (jiggleJump == 0) {
          jiggleJump += catalog.getSquashedRectangleAtIndex(index + 1).getHeight();
        }
      }
    }

    return jiggleJump;
  }

  public int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction) {

    Dimension preferredSize;
    Rectangle viewRectangle;

    viewRectangle = ((JViewport)catalog.getParent()).getViewRect();

    if (direction < 0) {
      return (int)viewRectangle.getY();
    }
    else if (direction > 0) {
      preferredSize = catalog.getPreferredSize();

      return (int)(preferredSize.getHeight() - viewRectangle.getY() + viewRectangle.getHeight());
    }

    return 0;
  }

}
