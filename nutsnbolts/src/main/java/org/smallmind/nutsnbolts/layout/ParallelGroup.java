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

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class ParallelGroup<C> extends Group<C, ParallelGroup> {

  private Alignment alignment;
  private Double minimumOverrideMeasurement;
  private Double preferredOverrideMeasurement;
  private Double maximumOverrideMeasurement;

  public ParallelGroup (ParaboxLayout<C> layout, Bias bias) {

    this(layout, bias, Alignment.CENTER);
  }

  public ParallelGroup (ParaboxLayout<C> layout, Bias bias, Alignment alignment) {

    super(layout, bias);

    this.alignment = alignment;
  }

  public Alignment getAlignment () {

    return alignment;
  }

  public ParallelGroup setAlignment (Alignment alignment) {

    this.alignment = alignment;

    return this;
  }

  public Double getMinimumOverrideMeasurement () {

    return minimumOverrideMeasurement;
  }

  public ParallelGroup setMinimumOverrideMeasurement (Double minimumOverrideMeasurement) {

    this.minimumOverrideMeasurement = minimumOverrideMeasurement;

    return this;
  }

  public Double getPreferredOverrideMeasurement () {

    return preferredOverrideMeasurement;
  }

  public ParallelGroup setPreferredOverrideMeasurement (Double preferredOverrideMeasurement) {

    this.preferredOverrideMeasurement = preferredOverrideMeasurement;

    return this;
  }

  public Double getMaximumOverrideMeasurement () {

    return maximumOverrideMeasurement;
  }

  public ParallelGroup setMaximumOverrideMeasurement (Double maximumOverrideMeasurement) {

    this.maximumOverrideMeasurement = maximumOverrideMeasurement;

    return this;
  }

  public double calculateMinimumMeasurement () {

    return calculateMeasurement(TapeMeasure.MINIMUM, minimumOverrideMeasurement);
  }

  public double calculatePreferredMeasurement () {

    return calculateMeasurement(TapeMeasure.PREFERRED, preferredOverrideMeasurement);
  }

  public double calculateMaximumMeasurement () {

    return calculateMeasurement(TapeMeasure.MAXIMUM, maximumOverrideMeasurement);
  }

  private synchronized double calculateMeasurement (TapeMeasure tapeMeasure, Double unbiasedMeasurementOverride) {

    double maxAscent = 0;
    double maxDescent = 0;

    for (ParaboxElement<?> element : getElements()) {

      double currentMeasurement = (unbiasedMeasurementOverride != null) ? unbiasedMeasurementOverride : tapeMeasure.getMeasure(getBias(), element);
      double currentAscent = (!alignment.equals(Alignment.BASELINE)) ? currentMeasurement : element.getBaseline(getBias(), currentMeasurement);
      double currentDescent;

      if (currentAscent > maxAscent) {
        maxAscent = currentAscent;
      }
      if ((currentDescent = (currentMeasurement - currentAscent)) > maxDescent) {
        maxDescent = currentDescent;
      }
    }

    return maxAscent + maxDescent;
  }

  @Override
  public synchronized void doLayout (double containerPosition, double containerMeasurement, LayoutTailor tailor) {

    if (!getElements().isEmpty()) {

      BaselineCalculations baselineCalculations = (alignment.equals(Alignment.BASELINE)) ? new BaselineCalculations(getBias(), maximumOverrideMeasurement, containerMeasurement, getElements()) : null;
      double elementMeasurement;
      int index = 0;

      for (ParaboxElement<?> element : getElements()) {
        if (containerMeasurement <= (elementMeasurement = (minimumOverrideMeasurement != null) ? minimumOverrideMeasurement : element.getMinimumMeasurement(getBias()))) {
          tailor.applyLayout(getBias(), containerPosition, elementMeasurement, element);
        }
        else if (containerMeasurement <= (elementMeasurement = (maximumOverrideMeasurement != null) ? maximumOverrideMeasurement : element.getMaximumMeasurement(getBias()))) {
          tailor.applyLayout(getBias(), containerPosition, containerMeasurement, element);
        }
        else {
          switch (alignment) {
            case FIRST:
              tailor.applyLayout(getBias(), containerPosition, elementMeasurement, element);
              break;
            case LAST:
              tailor.applyLayout(getBias(), containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
              break;
            case LEADING:
              if (!getBias().equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
                tailor.applyLayout(getBias(), containerPosition, elementMeasurement, element);
              }
              else {
                switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                  case FIRST_TO_LAST:
                    tailor.applyLayout(getBias(), containerPosition, elementMeasurement, element);
                    break;
                  case LAST_TO_FIRST:
                    tailor.applyLayout(getBias(), containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
                }
              }
              break;
            case TRAILING:
              if (!getBias().equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
                tailor.applyLayout(getBias(), containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
              }
              else {
                switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                  case FIRST_TO_LAST:
                    tailor.applyLayout(getBias(), containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
                    break;
                  case LAST_TO_FIRST:
                    tailor.applyLayout(getBias(), containerPosition, elementMeasurement, element);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
                }
              }
              break;
            case CENTER:
              tailor.applyLayout(getBias(), containerPosition + ((containerMeasurement - elementMeasurement) / 2.0D), elementMeasurement, element);
              break;
            case BASELINE:

              if (baselineCalculations == null) {
                throw new NullPointerException();
              }

              double top;

              if ((top = baselineCalculations.getIdealizedBaseline() - baselineCalculations.getElementAscentsDescents()[index].getFirst()) < 0) {
                top = 0;
              }
              else if (top + elementMeasurement > containerMeasurement) {
                top += containerMeasurement - (top + elementMeasurement);
              }

              tailor.applyLayout(getBias(), containerPosition + top, elementMeasurement, element);
              break;
            default:
              throw new UnknownSwitchCaseException(alignment.name());
          }
        }
      }
    }
  }
}
