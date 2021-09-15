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
package org.smallmind.nutsnbolts.layout;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class ParallelBox extends Box<ParallelBox> {

  private Alignment alignment;
  private Double minimumOverrideMeasurement;
  private Double preferredOverrideMeasurement;
  private Double maximumOverrideMeasurement;

  // Lays out items one after another, in the direction *opposite* to the Bias, but with growth/shrink values still in the bias direction
  protected ParallelBox (ParaboxLayout layout) {

    this(layout, Alignment.LEADING);
  }

  protected ParallelBox (ParaboxLayout layout, Alignment alignment) {

    super(ParallelBox.class, layout);

    this.alignment = alignment;
  }

  public Alignment getAlignment () {

    return alignment;
  }

  public ParallelBox setAlignment (Alignment alignment) {

    this.alignment = alignment;

    return this;
  }

  public Double getMinimumOverrideMeasurement () {

    return minimumOverrideMeasurement;
  }

  public ParallelBox setMinimumOverrideMeasurement (Double minimumOverrideMeasurement) {

    this.minimumOverrideMeasurement = minimumOverrideMeasurement;

    return this;
  }

  public Double getPreferredOverrideMeasurement () {

    return preferredOverrideMeasurement;
  }

  public ParallelBox setPreferredOverrideMeasurement (Double preferredOverrideMeasurement) {

    this.preferredOverrideMeasurement = preferredOverrideMeasurement;

    return this;
  }

  public Double getMaximumOverrideMeasurement () {

    return maximumOverrideMeasurement;
  }

  public ParallelBox setMaximumOverrideMeasurement (Double maximumOverrideMeasurement) {

    this.maximumOverrideMeasurement = maximumOverrideMeasurement;

    return this;
  }

  @Override
  public double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MINIMUM, minimumOverrideMeasurement, tailor);
  }

  @Override
  public double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.PREFERRED, preferredOverrideMeasurement, tailor);
  }

  @Override
  public double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MAXIMUM, maximumOverrideMeasurement, tailor);
  }

  private synchronized double calculateMeasurement (Bias bias, TapeMeasure tapeMeasure, Double unbiasedMeasurementOverride, LayoutTailor tailor) {

    double maxAscent = 0;
    double maxDescent = 0;

    if (!getElements().isEmpty()) {
      for (ParaboxElement<?> element : getElements()) {

        double currentMeasurement = (unbiasedMeasurementOverride != null) ? unbiasedMeasurementOverride : tapeMeasure.getMeasure(bias, element, tailor);
        double currentAscent = (!alignment.equals(Alignment.BASELINE)) ? currentMeasurement : element.getBaseline(bias, currentMeasurement);
        double currentDescent;

        if (currentAscent > maxAscent) {
          maxAscent = currentAscent;
        }
        if ((currentDescent = (currentMeasurement - currentAscent)) > maxDescent) {
          maxDescent = currentDescent;
        }
      }
    }

    return maxAscent + maxDescent;
  }

  @Override
  public synchronized void doLayout (Bias bias, double containerPosition, double containerMeasurement, LayoutTailor tailor) {

    if (!getElements().isEmpty()) {

      BaselineCalculations baselineCalculations = (alignment.equals(Alignment.BASELINE)) ? new BaselineCalculations(bias, maximumOverrideMeasurement, containerMeasurement, getElements(), tailor) : null;
      double elementMeasurement;
      int index = 0;

      for (ParaboxElement<?> element : getElements()) {
        if (containerMeasurement <= (elementMeasurement = (minimumOverrideMeasurement != null) ? minimumOverrideMeasurement : element.getMinimumMeasurement(bias, tailor))) {
          tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
        } else if (containerMeasurement <= (elementMeasurement = (maximumOverrideMeasurement != null) ? maximumOverrideMeasurement : element.getMaximumMeasurement(bias, tailor))) {
          tailor.applyLayout(bias, containerPosition, containerMeasurement, element);
        } else {
          switch (alignment) {
            case FIRST:
              tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
              break;
            case LAST:
              tailor.applyLayout(bias, containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
              break;
            case LEADING:
              if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
                tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
              } else {
                switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                  case FIRST_TO_LAST:
                    tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
                    break;
                  case LAST_TO_FIRST:
                    tailor.applyLayout(bias, containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
                }
              }
              break;
            case TRAILING:
              if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
                tailor.applyLayout(bias, containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
              } else {
                switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                  case FIRST_TO_LAST:
                    tailor.applyLayout(bias, containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
                    break;
                  case LAST_TO_FIRST:
                    tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
                }
              }
              break;
            case CENTER:
              tailor.applyLayout(bias, containerPosition + ((containerMeasurement - elementMeasurement) / 2.0D), elementMeasurement, element);
              break;
            case BASELINE:

              if (baselineCalculations == null) {
                throw new NullPointerException();
              }

              double top;

              if ((top = baselineCalculations.getIdealizedBaseline() - baselineCalculations.getElementAscentsDescents()[index].getFirst()) < 0) {
                top = 0;
              } else if (top + elementMeasurement > containerMeasurement) {
                top = containerMeasurement - elementMeasurement;
              }

              tailor.applyLayout(bias, containerPosition + top, elementMeasurement, element);
              break;
            default:
              throw new UnknownSwitchCaseException(alignment.name());
          }
        }
      }
    }
  }
}
