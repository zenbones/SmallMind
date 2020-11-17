/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.swing;

import java.awt.Color;
import javax.swing.UIManager;

public class ColorUtility {

  /*
   desktop: Color of the desktop background
   activeCaption: Color for captions (title bars) when they are active.
   activeCaptionText: Text color for text in captions (title bars).
   activeCaptionBorder: Border color for caption (title bar) window borders.
   inactiveCaption: Color for captions (title bars) when not active.
   inactiveCaptionText: Text color for text in inactive captions (title bars).
   inactiveCaptionBorder: Border color for inactive caption (title bar) window borders.
   window: Default color for the interior of windows
   windowBorder: Color of the window's border
   windowText: Color of the window's title text
   menu: Background color for menus
   menuText: Text color for menus
   text: Text background color
   textText: Text foreground color
   textHighlight: Text background color when selected
   textHighlightText: Text color when selected
   textInactiveText: Text color when disabled
   control: Default color for controls (buttons, sliders, etc)
   controlText: Default color for text in controls
   controlHighlight: Specular highlight (opposite of the shadow)
   controlLtHighlight: Highlight color for controls
   controlShadow: Shadow color for controls
   controlDkShadow: Dark shadow color for controls
   scrollbar: Scrollbar background (usually the "track")
   info: information color (sometimes used for tooltips)
   infoText: information text color (sometimes used for tooltips)
  */

  public static final Color TEXT_COLOR = UIManager.getDefaults().getColor("text");
  public static final Color INVERSE_TEXT_COLOR = invert(TEXT_COLOR);
  public static final Color TEXT_TEXT_COLOR = UIManager.getDefaults().getColor("textText");
  public static final Color INVERSE_TEXT_TEXT_COLOR = invert(TEXT_TEXT_COLOR);
  public static final Color HIGHLIGHT_COLOR = new Color(178, 178, 255);
  public static final Color INVERSE_HIGHLIGHT_COLOR = new Color(255, 222, 100);

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
    } else {
      tingedChannel = channel + (int)Math.floor(step * (tintChannel / 255F));
    }

    if (tingedChannel < 0) {
      return 0;
    } else if (tingedChannel > 255) {
      return 255;
    } else {
      return tingedChannel;
    }
  }
}
