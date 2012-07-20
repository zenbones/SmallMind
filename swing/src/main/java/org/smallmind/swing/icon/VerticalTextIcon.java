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
package org.smallmind.swing.icon;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

public class VerticalTextIcon implements Icon {

  public static final int ROTATE_DEFAULT = 0x00;
  public static final int ROTATE_NONE = 0x01;
  public static final int ROTATE_LEFT = 0x02;
  public static final int ROTATE_RIGHT = 0x04;

  private static final String sDrawsInTopRight = "\u3041\u3043\u3045\u3047\u3049\u3063\u3083\u3085\u3087\u308E" + // hiragana
    "\u30A1\u30A3\u30A5\u30A7\u30A9\u30C3\u30E3\u30E5\u30E7\u30EE\u30F5\u30F6"; // katakana
  private static final String sDrawsInFarTopRight = "\u3001\u3002"; // comma, full stop

  private static final double NINETY_DEGREES = Math.toRadians(90.0);

  private static final int POSITION_NORMAL = 0;
  private static final int POSITION_TOP_RIGHT = 1;
  private static final int POSITION_FAR_TOP_RIGHT = 2;

  private static final int kBufferSpace = 5;

  private FontMetrics fontMetrics;
  private String text;
  private String[] fCharStrings; // for efficiency, break the text into one-char strings to be passed to drawString
  private int[] fCharWidths; // Roman characters should be centered when not rotated (Japanese fonts are monospaced)
  private int[] fPosition; // Japanese half-height characters need to be shifted when drawn vertically
  private int rotation;
  private int fWidth, fHeight, fCharHeight, fDescent;

  public VerticalTextIcon (FontMetrics fontMetrics, String text, int rotation) {

    this.fontMetrics = fontMetrics;
    this.text = text;
    this.rotation = rotation;

    calcDimensions();
  }

  public void setLabel (String text) {

    this.text = text;

    calcDimensions();
  }

  public int getIconWidth () {

    return fWidth;
  }

  public int getIconHeight () {

    return fHeight;
  }

  void calcDimensions () {

    fCharHeight = fontMetrics.getAscent() + fontMetrics.getDescent();
    fDescent = fontMetrics.getDescent();
    if (rotation == ROTATE_NONE) {
      int len = text.length();
      char data[] = new char[len];
      text.getChars(0, len, data, 0);
      // if not rotated, width is that of the widest char in the string
      fWidth = 0;
      // we need an array of one-char strings for drawString
      fCharStrings = new String[len];
      fCharWidths = new int[len];
      fPosition = new int[len];
      char ch;
      for (int i = 0; i < len; i++) {
        ch = data[i];
        fCharWidths[i] = fontMetrics.charWidth(ch);
        if (fCharWidths[i] > fWidth) {
          fWidth = fCharWidths[i];
        }
        fCharStrings[i] = new String(data, i, 1);
        // small kana and punctuation
        if (sDrawsInTopRight.indexOf(ch) >= 0) // if ch is in sDrawsInTopRight
        {
          fPosition[i] = POSITION_TOP_RIGHT;
        }
        else if (sDrawsInFarTopRight.indexOf(ch) >= 0) {
          fPosition[i] = POSITION_FAR_TOP_RIGHT;
        }
        else {
          fPosition[i] = POSITION_NORMAL;
        }
      }
      // and height is the font height * the char count, + one extra leading at the bottom
      fHeight = fCharHeight * len + fDescent;
    }
    else {
      // if rotated, width is the height of the string
      fWidth = fCharHeight;
      // and height is the width, plus some buffer space
      fHeight = fontMetrics.stringWidth(text) + 2 * kBufferSpace;
    }
  }

  public void paintIcon (Component c, Graphics g, int x, int y) {

    g.setColor(c.getForeground());
    g.setFont(c.getFont());
    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    if (rotation == ROTATE_NONE) {
      int yPos = y + fCharHeight;
      for (int i = 0; i < fCharStrings.length; i++) {
        // Special rules for Japanese - "half-height" characters (like ya, yu, yo in combinations)
        // should draw in the top-right quadrant when drawn vertically
        // - they draw in the bottom-left normally
        int tweak;
        switch (fPosition[i]) {
          case POSITION_NORMAL:
            // Roman fonts should be centered. Japanese fonts are always monospaced.
            g.drawString(fCharStrings[i], x + ((fWidth - fCharWidths[i]) / 2), yPos);
            break;
          case POSITION_TOP_RIGHT:
            tweak = fCharHeight / 3; // Should be 2, but they aren't actually half-height
            g.drawString(fCharStrings[i], x + (tweak / 2), yPos - tweak);
            break;
          case POSITION_FAR_TOP_RIGHT:
            tweak = fCharHeight - fCharHeight / 3;
            g.drawString(fCharStrings[i], x + (tweak / 2), yPos - tweak);
            break;
        }
        yPos += fCharHeight;
      }
    }
    else if (rotation == ROTATE_LEFT) {
      g.translate(x + fWidth, y + fHeight);
      ((Graphics2D)g).rotate(-NINETY_DEGREES);
      g.drawString(text, kBufferSpace, -fDescent);
      ((Graphics2D)g).rotate(NINETY_DEGREES);
      g.translate(-(x + fWidth), -(y + fHeight));
    }
    else if (rotation == ROTATE_RIGHT) {
      g.translate(x, y);
      ((Graphics2D)g).rotate(NINETY_DEGREES);
      g.drawString(text, kBufferSpace, -fDescent);
      ((Graphics2D)g).rotate(-NINETY_DEGREES);
      g.translate(-x, -y);
    }
  }
}

