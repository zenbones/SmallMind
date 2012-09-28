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

public enum Bias {

  HORIZONTAL {
    @Override
    public Pair getBiasedPair (double biasedMeasurement, double unbiasedMeasurement) {

      return new Pair(biasedMeasurement, unbiasedMeasurement);
    }

    @Override
    public double getBiasedMeasurement (double width, double height) {

      return width;
    }

    @Override
    public double getUnbiasedMeasurement (double width, double height) {

      return height;
    }

    @Override
    public double getMinimumBiasedMeasurement (ParaboxElement<?> element) {

      return element.getMaximumWidth();
    }

    @Override
    public double getMinimumUnbiasedMeasurement (ParaboxElement<?> element) {

      return element.getMinimumHeight();
    }

    @Override
    public double getPreferredBiasedMeasurement (ParaboxElement<?> element) {

      return element.getPreferredWidth();
    }

    @Override
    public double getPreferredUnbiasedMeasurement (ParaboxElement<?> element) {

      return element.getPreferredHeight();
    }

    @Override
    public double getMaximumBiasedMeasurement (ParaboxElement<?> element) {

      return element.getMaximumWidth();
    }

    @Override
    public double getMaximumUnbiasedMeasurement (ParaboxElement<?> element) {

      return element.getMaximumHeight();
    }

    @Override
    public double getBiasedGrow (ParaboxElement<?> element) {

      return element.getConstraint().getGrowX();
    }

    @Override
    public double getBiasedShrink (ParaboxElement<?> element) {

      return element.getConstraint().getShrinkX();
    }
  },
  VERTICAL {
    @Override
    public Pair getBiasedPair (double biasedMeasurement, double unbiasedMeasurement) {

      return new Pair(unbiasedMeasurement, biasedMeasurement);
    }

    @Override
    public double getBiasedMeasurement (double width, double height) {

      return height;
    }

    @Override
    public double getUnbiasedMeasurement (double width, double height) {

      return width;
    }

    @Override
    public double getMinimumBiasedMeasurement (ParaboxElement<?> element) {

      return element.getMaximumHeight();
    }

    @Override
    public double getMinimumUnbiasedMeasurement (ParaboxElement<?> element) {

      return element.getMinimumWidth();
    }

    @Override
    public double getPreferredBiasedMeasurement (ParaboxElement<?> element) {

      return element.getPreferredHeight();
    }

    @Override
    public double getPreferredUnbiasedMeasurement (ParaboxElement<?> element) {

      return element.getPreferredWidth();
    }

    @Override
    public double getMaximumBiasedMeasurement (ParaboxElement<?> element) {

      return element.getMaximumHeight();
    }

    @Override
    public double getMaximumUnbiasedMeasurement (ParaboxElement<?> element) {

      return element.getMaximumWidth();
    }

    @Override
    public double getBiasedGrow (ParaboxElement<?> element) {

      return element.getConstraint().getGrowY();
    }

    @Override
    public double getBiasedShrink (ParaboxElement<?> element) {

      return element.getConstraint().getShrinkY();
    }
  };

  public abstract Pair getBiasedPair (double biasedMeasurement, double unbiasedMeasurement);

  public abstract double getBiasedMeasurement (double width, double height);

  public abstract double getUnbiasedMeasurement (double width, double height);

  public abstract double getMinimumBiasedMeasurement (ParaboxElement<?> element);

  public abstract double getMinimumUnbiasedMeasurement (ParaboxElement<?> element);

  public abstract double getPreferredBiasedMeasurement (ParaboxElement<?> element);

  public abstract double getPreferredUnbiasedMeasurement (ParaboxElement<?> element);

  public abstract double getMaximumBiasedMeasurement (ParaboxElement<?> element);

  public abstract double getMaximumUnbiasedMeasurement (ParaboxElement<?> element);

  public abstract double getBiasedGrow (ParaboxElement<?> element);

  public abstract double getBiasedShrink (ParaboxElement<?> element);
}
