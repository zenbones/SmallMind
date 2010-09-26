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
package org.smallmind.swing.signal;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class TrafficLight extends Box {

   public static final int X_AXIS = BoxLayout.X_AXIS;
   public static final int Y_AXIS = BoxLayout.Y_AXIS;

   private static ImageIcon CLEAR_LIGHT;
   private static ImageIcon RED_LIGHT;
   private static ImageIcon YELLOW_LIGHT;
   private static ImageIcon GREEN_LIGHT;

   private JLabel redLightLabel;
   private JLabel yellowLightLabel;
   private JLabel greenLightLabel;
   private ReadySetGo readySetGo;

   static {

      CLEAR_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_clear.png"));
      RED_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_red.png"));
      YELLOW_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_yellow.png"));
      GREEN_LIGHT = new ImageIcon(ClassLoader.getSystemResource("public/images/bullet_ball_glass_green.png"));
   }

   public TrafficLight (int axis) {

      super(axis);

      readySetGo = ReadySetGo.RED;

      redLightLabel = new JLabel(RED_LIGHT);
      yellowLightLabel = new JLabel(CLEAR_LIGHT);
      greenLightLabel = new JLabel(CLEAR_LIGHT);

      if (axis == X_AXIS) {
         add(redLightLabel);
         add(createHorizontalStrut(3));
         add(yellowLightLabel);
         add(createHorizontalStrut(3));
         add(greenLightLabel);
      }
      else {
         add(greenLightLabel);
         add(createVerticalStrut(3));
         add(yellowLightLabel);
         add(createVerticalStrut(3));
         add(redLightLabel);
      }
   }

   public synchronized void inc () {

      readySetGo = readySetGo.inc();

      switch (readySetGo) {
         case YELLOW:
            redLightLabel.setIcon(CLEAR_LIGHT);
            yellowLightLabel.setIcon(YELLOW_LIGHT);
            break;
         case GREEN:
            yellowLightLabel.setIcon(CLEAR_LIGHT);
            greenLightLabel.setIcon(GREEN_LIGHT);
      }
   }

}
