/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.swing.icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

public class SwatchIcon implements Icon {

  private Color color;
  private int size;

  public SwatchIcon (int size, Color color) {

    this.size = size;
    this.color = color;
  }

  @Override
  public int getIconWidth () {

    return size;
  }

  @Override
  public int getIconHeight () {

    return size;
  }

  @Override
  public void paintIcon (Component component, Graphics g, int x, int y) {

    Color oldColor = g.getColor();

    g.setColor(color);
    g.fillRect(x, y, size, size);

    g.setColor(Color.BLACK);
    g.drawRect(x, y, size, size);

    g.setColor(oldColor);
  }
}
