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
