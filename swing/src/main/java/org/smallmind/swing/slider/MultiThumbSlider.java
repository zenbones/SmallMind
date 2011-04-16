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
package org.smallmind.swing.slider;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.SystemColor;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

public class MultiThumbSlider extends JComponent {

  private static final ImageIcon THUMB_ICON = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/thumb_16.png"));

  public static final int HORIZONTAL = SwingConstants.HORIZONTAL;
  public static final int VERTICAL = SwingConstants.VERTICAL;

  private int orientation = HORIZONTAL;
  private int minValue;
  private int maxValue;

  public Dimension getPreferredSize () {

    if (orientation == HORIZONTAL) {
      return new Dimension(0, 20);
    }
    else {
      return new Dimension(20, 0);
    }
  }

  public Dimension getMaximumSize () {

    if (orientation == HORIZONTAL) {
      return new Dimension(Short.MAX_VALUE, 20);
    }
    else {
      return new Dimension(20, Short.MAX_VALUE);
    }
  }

  public void paint (Graphics g) {

    Graphics2D g2 = (Graphics2D)g;
    Dimension currentSize = getSize();

    paintTrack(g);
    paintThumb(g);
    paintBorder(g);
  }

  private void paintThumb (Graphics g) {

    g.setColor(Color.RED);
    g.drawRect(300, 0, 16, 16);
  }

  private void paintTrack (Graphics g) {

    Dimension currentSize = getSize();

    if (orientation == HORIZONTAL) {
      g.setColor(SystemColor.controlText);
      g.drawRect(0, 5, (int)currentSize.getWidth(), 5);
      g.setColor(SystemColor.controlShadow);
      g.drawLine(1, 6, (int)currentSize.getWidth() - 1, 6);
    }
    else {
      g.setColor(SystemColor.controlText);
      g.drawRect(5, 0, 5, (int)currentSize.getHeight());
      g.setColor(SystemColor.controlShadow);
      g.drawLine(6, 1, 6, (int)currentSize.getHeight() - 1);
    }
  }
}
