/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.swing.signal;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class IndicatorBall extends JLabel {

  private static final ImageIcon RED_BALL = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/ball_red_16.png"));
  private static final ImageIcon YELLOW_BALL = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/ball_yellow_16.png"));
  private static final ImageIcon GREEN_BALL = new ImageIcon(ClassLoader.getSystemResource("org/smallmind/swing/system/ball_green_16.png"));

  private ReadySetGo color;

  public IndicatorBall (ReadySetGo color) {

    super();

    setColor(color);
  }

  public synchronized ReadySetGo getColor () {

    return color;
  }

  public synchronized void setColor (ReadySetGo color) {

    this.color = color;

    switch (color) {
      case RED:
        setIcon(RED_BALL);
        break;
      case YELLOW:
        setIcon(YELLOW_BALL);
        break;
      case GREEN:
        setIcon(GREEN_BALL);
        break;
      default:
        throw new UnknownSwitchCaseException(color.name());
    }
  }
}
