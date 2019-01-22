/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

public class SmallMindGrayFilter extends RGBImageFilter {

  private boolean brighter;
  private int percent;

  public static Image createDisabledImage (Image image) {

    SmallMindGrayFilter filter = new SmallMindGrayFilter(true, 50);
    ImageProducer prod = new FilteredImageSource(image.getSource(), filter);

    return Toolkit.getDefaultToolkit().createImage(prod);
  }

  /**
   * Constructs a GrayFilter object that filters a color image to a
   * grayscale image. Used by buttons to create disabled ("grayed out")
   * button images.
   *
   * @param brighter a boolean -- true if the pixels should be brightened
   * @param percent  an int in the range 0..100 that determines the percentage
   *                 of gray, where 100 is the darkest gray, and 0 is the lightest
   */
  public SmallMindGrayFilter (boolean brighter, int percent) {

    this.brighter = brighter;
    this.percent = percent;

    // canFilterIndexColorModel indicates whether or not it is acceptable
    // to apply the color filtering of the filterRGB method to the color
    // table entries of an IndexColorModel object in lieu of pixel by pixel
    // filtering.
    canFilterIndexColorModel = true;
  }

  public int filterRGB (int x, int y, int rgb) {

    int gray = ((((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3);

    if (brighter) {
      gray = (255 - ((255 - gray) * (100 - percent) / 100));
    }
    else {
      gray = (gray * (100 - percent) / 100);
    }

    if (gray < 0) gray = 0;
    if (gray > 255) gray = 255;

    return (rgb & 0xff000000) | (gray << 16) | (gray << 8) | (gray);
  }
}

