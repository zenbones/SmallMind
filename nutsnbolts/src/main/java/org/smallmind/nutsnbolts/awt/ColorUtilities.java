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
package org.smallmind.nutsnbolts.awt;

import java.awt.Color;

public class ColorUtilities {

   public static Color invert (Color color) {

      return invert(color, color.getAlpha());
   }

   public static Color invert (Color color, int alpha) {

      return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue(), alpha);
   }

   public static Color shade (Color color, Color tint, int step) {

      return new Color(tinge(color.getRed(), tint.getRed(), step), tinge(color.getGreen(), tint.getGreen(), step), tinge(color.getBlue(), tint.getBlue(), step), color.getAlpha());
   }

   private static int tinge (int channel, int tintChannel, int step) {

      int tingedChannel;

      if (channel >= tintChannel) {
         tingedChannel = channel - (int)Math.floor(step * ((255 - tintChannel) / 255F));
      }
      else {
         tingedChannel = channel + (int)Math.floor(step * (tintChannel / 255F));
      }

      if (tingedChannel < 0) {
         return 0;
      }
      else if (tingedChannel > 255) {
         return 255;
      }
      else {
         return tingedChannel;
      }

   }

}
