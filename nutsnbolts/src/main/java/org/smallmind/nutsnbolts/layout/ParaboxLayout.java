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
package org.smallmind.nutsnbolts.layout;

import java.util.Collection;

public class ParaboxLayout<E extends ParaboxElement<?>> {

  private ParaboxContainer container;
  private Bias bias;
  private Alignment biasedAlignment;
  private Alignment unbiasedAlignment;
  private Double unbiasedMinimumMeasurement;
  private Double unbiasedPreferredMeasurement;
  private Double unbiasedMaximumMeasurement;
  private double gap;

  public ParaboxLayout (ParaboxContainer container) {

    this(container, Bias.HORIZONTAL);
  }

  public ParaboxLayout (ParaboxContainer container, Bias bias) {

    this(container, bias, Gap.RELATED);
  }

  public ParaboxLayout (ParaboxContainer container, Bias bias, Gap gap) {

    this(container, bias, gap.getGap(container.getPlatform()));
  }

  public ParaboxLayout (ParaboxContainer container, Bias bias, double gap) {

    this(container, bias, gap, Alignment.LEADING, Alignment.CENTER);
  }

  public ParaboxLayout (ParaboxContainer container, Bias bias, Gap gap, Alignment biasedAlignment, Alignment unbiasedAlignment) {

    this(container, bias, gap.getGap(container.getPlatform()), biasedAlignment, unbiasedAlignment);
  }

  public ParaboxLayout (ParaboxContainer container, Bias bias, double gap, Alignment biasedAlignment, Alignment unbiasedAlignment) {

    this.container = container;
    this.bias = bias;
    this.biasedAlignment = biasedAlignment;
    this.unbiasedAlignment = unbiasedAlignment;
    this.gap = gap;
  }

  public Bias getBias () {

    return bias;
  }

  public ParaboxLayout<E> setBias (Bias bias) {

    this.bias = bias;

    return this;
  }

  public double getGap () {

    return gap;
  }

  public ParaboxLayout<E> setGap (Gap gap) {

    return setGap(gap.getGap(container.getPlatform()));
  }

  public ParaboxLayout<E> setGap (double gap) {

    this.gap = gap;

    return this;
  }

  public Alignment getBiasedAlignment () {

    return biasedAlignment;
  }

  public ParaboxLayout<E> setBiasedAlignment (Alignment biasedAlignment) {

    this.biasedAlignment = biasedAlignment;

    return this;
  }

  public Alignment getUnbiasedAlignment () {

    return unbiasedAlignment;
  }

  public ParaboxLayout<E> setUnbiasedAlignment (Alignment unbiasedAlignment) {

    this.unbiasedAlignment = unbiasedAlignment;

    return this;
  }

  public Double getUnbiasedMinimumMeasurement () {

    return unbiasedMinimumMeasurement;
  }

  public ParaboxLayout<E> setUnbiasedMinimumMeasurement (Double unbiasedMinimumMeasurement) {

    this.unbiasedMinimumMeasurement = unbiasedMinimumMeasurement;

    return this;
  }

  public Double getUnbiasedPreferredMeasurement () {

    return unbiasedPreferredMeasurement;
  }

  public ParaboxLayout<E> setUnbiasedPreferredMeasurement (Double unbiasedPreferredMeasurement) {

    this.unbiasedPreferredMeasurement = unbiasedPreferredMeasurement;

    return this;
  }

  public Double getUnbiasedMaximumMeasurement () {

    return unbiasedMaximumMeasurement;
  }

  public ParaboxLayout<E> setUnbiasedMaximumMeasurement (Double unbiasedMaximumMeasurement) {

    this.unbiasedMaximumMeasurement = unbiasedMaximumMeasurement;

    return this;
  }

  public Size calculateMinimumSize (Collection<E> elements) {

    return getBias().getSize(calculateMinimumBiasedMeasurement(elements), calculateMinimumUnbiasedMeasurement(elements));
  }

  private double calculateMinimumBiasedMeasurement (Collection<E> elements) {

    boolean first = true;
    double total = 0.0D;

    for (E element : elements) {
      total += getBias().getMinimumBiasedMeasurement(element);
      if (!first) {
        total += gap;
      }
      first = false;
    }

    return total;
  }

  private double calculateMinimumUnbiasedMeasurement (Collection<E> elements) {

    if (unbiasedMinimumMeasurement != null) {

      return unbiasedMaximumMeasurement;
    }
    else {

      double greatest = 0;

      for (E element : elements) {

        double current;

        if ((current = getBias().getMinimumUnbiasedMeasurement(element)) > greatest) {
          greatest = current;
        }
      }

      return greatest;
    }
  }

  public Size calculatePreferredSize (Collection<E> elements) {

    return getBias().getSize(calculatePreferredBiasedMeasurement(elements), calculatePreferredUnbiasedMeasurement(elements));
  }

  private double calculatePreferredBiasedMeasurement (Collection<E> elements) {

    boolean first = true;
    double total = 0;

    for (E element : elements) {
      total += getBias().getPreferredBiasedMeasurement(element);
      if (!first) {
        total += gap;
      }
      first = false;
    }

    return total;
  }

  private double calculatePreferredUnbiasedMeasurement (Collection<E> elements) {

    if (unbiasedPreferredMeasurement != null) {

      return unbiasedPreferredMeasurement;
    }
    else {

      double greatest = 0;

      for (E element : elements) {

        double current;

        if ((current = getBias().getPreferredUnbiasedMeasurement(element)) > greatest) {
          greatest = current;
        }
      }

      return greatest;
    }
  }

  public void doLayout (double width, double height, Collection<E> elements) {

  }
}
