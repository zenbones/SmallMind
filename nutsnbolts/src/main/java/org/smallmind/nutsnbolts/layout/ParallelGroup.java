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

import java.util.List;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class ParallelGroup extends Group<ParallelGroup> {

  private Alignment alignment;
  private Double minimumUnbiasedMeasurement;
  private Double preferredUnbiasedMeasurement;
  private Double maximumUnbiasedMeasurement;

  public ParallelGroup (Bias bias) {

    this(bias, Alignment.CENTER);
  }

  public ParallelGroup (Bias bias, Alignment alignment) {

    super(bias);

    this.alignment = alignment;
  }

  public Alignment getAlignment () {

    return alignment;
  }

  public ParallelGroup setAlignment (Alignment alignment) {

    this.alignment = alignment;

    return this;
  }

  public Double getMinimumUnbiasedMeasurement () {

    return minimumUnbiasedMeasurement;
  }

  public ParallelGroup setMinimumUnbiasedMeasurement (Double minimumUnbiasedMeasurement) {

    this.minimumUnbiasedMeasurement = minimumUnbiasedMeasurement;

    return this;
  }

  public Double getPreferredUnbiasedMeasurement () {

    return preferredUnbiasedMeasurement;
  }

  public ParallelGroup setPreferredUnbiasedMeasurement (Double preferredUnbiasedMeasurement) {

    this.preferredUnbiasedMeasurement = preferredUnbiasedMeasurement;

    return this;
  }

  public Double getMaximumUnbiasedMeasurement () {

    return maximumUnbiasedMeasurement;
  }

  public ParallelGroup setMaximumUnbiasedMeasurement (Double maximumUnbiasedMeasurement) {

    this.maximumUnbiasedMeasurement = maximumUnbiasedMeasurement;

    return this;
  }

  public Pair calculateMinimumContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculateBiasedContainerMeasurement(TapeMeasure.MINIMUM, elements), calculateUnbiasedContainerMeasurement(TapeMeasure.MINIMUM, minimumUnbiasedMeasurement, elements));
  }

  public Pair calculatePreferredContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculateBiasedContainerMeasurement(TapeMeasure.PREFERRED, elements), calculateUnbiasedContainerMeasurement(TapeMeasure.PREFERRED, preferredUnbiasedMeasurement, elements));
  }

  public Pair calculateMaximumContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculateBiasedContainerMeasurement(TapeMeasure.MAXIMUM, elements), calculateUnbiasedContainerMeasurement(TapeMeasure.MAXIMUM, maximumUnbiasedMeasurement, elements));
  }

  private double calculateContainerMeasurement (TapeMeasure tapeMeasure, Double unbiasedMeasurementOverride, List<E> elements) {

    double maxAscent = 0;
    double maxDescent = 0;

    for (E element : elements) {

      double currentMeasurement = (unbiasedMeasurementOverride != null) ? unbiasedMeasurementOverride : tapeMeasure.getUnbiasedMeasure(bias, element);
      double currentAscent = (!alignment.equals(Alignment.BASELINE)) ? currentMeasurement : element.getBaseline(bias, currentMeasurement);
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

  private PartialSolution[] doLayout (double unbiasedContainerMeasurement, List<E> elements) {

    PartialSolution[] partialSolutions = new PartialSolution[(elements == null) ? 0 : elements.size()];

    if (elements != null) {

      BaselineCalculations<E> baselineCalculations = (alignment.equals(Alignment.BASELINE)) ? new BaselineCalculations<E>(bias, maximumUnbiasedMeasurement, unbiasedContainerMeasurement, elements) : null;
      double elementMeasurement;
      int index = 0;

      for (E element : elements) {
        if (unbiasedContainerMeasurement <= (elementMeasurement = (minimumUnbiasedMeasurement != null) ? minimumUnbiasedMeasurement : getBias().getMinimumUnbiasedMeasurement(element))) {
          partialSolutions[index++] = new PartialSolution(0, elementMeasurement);
        }
        else if (unbiasedContainerMeasurement <= (elementMeasurement = (maximumUnbiasedMeasurement != null) ? maximumUnbiasedMeasurement : bias.getMaximumUnbiasedMeasurement(element))) {
          partialSolutions[index++] = new PartialSolution(0, unbiasedContainerMeasurement);
        }
        else {
          switch (alignment) {
            case FIRST:
              partialSolutions[index++] = new PartialSolution(0, elementMeasurement);
              break;
            case LAST:
              partialSolutions[index++] = new PartialSolution(unbiasedContainerMeasurement - elementMeasurement, elementMeasurement);
              break;
            case LEADING:
              if (!bias.equals(container.getPlatform().getOrientation().getBias())) {
                partialSolutions[index++] = new PartialSolution(0, elementMeasurement);
              }
              else {
                switch (container.getPlatform().getOrientation().getFlow()) {
                  case FIRST_TO_LAST:
                    partialSolutions[index++] = new PartialSolution(0, elementMeasurement);
                    break;
                  case LAST_TO_FIRST:
                    partialSolutions[index++] = new PartialSolution(unbiasedContainerMeasurement - elementMeasurement, elementMeasurement);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(container.getPlatform().getOrientation().getFlow().name());
                }
              }
              break;
            case TRAILING:
              if (!bias.equals(container.getPlatform().getOrientation().getBias())) {
                partialSolutions[index++] = new PartialSolution(unbiasedContainerMeasurement - elementMeasurement, elementMeasurement);
              }
              else {
                switch (container.getPlatform().getOrientation().getFlow()) {
                  case FIRST_TO_LAST:
                    partialSolutions[index++] = new PartialSolution(unbiasedContainerMeasurement - elementMeasurement, elementMeasurement);
                    break;
                  case LAST_TO_FIRST:
                    partialSolutions[index++] = new PartialSolution(0, elementMeasurement);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(container.getPlatform().getOrientation().getFlow().name());
                }
              }
              break;
            case CENTER:
              partialSolutions[index++] = new PartialSolution((unbiasedContainerMeasurement - elementMeasurement) / 2.0D, elementMeasurement);
              break;
            case BASELINE:

              if (baselineCalculations == null) {
                throw new NullPointerException();
              }

              double top;

              if ((top = baselineCalculations.getIdealizedBaseline() - baselineCalculations.getElementAscentsDescents()[index].getFirst()) < 0) {
                top = 0;
              }
              else if (top + elementMeasurement > unbiasedContainerMeasurement) {
                top += unbiasedContainerMeasurement - (top + elementMeasurement);
              }

              partialSolutions[index++] = new PartialSolution(top, elementMeasurement);
              break;
            default:
              throw new UnknownSwitchCaseException(alignment.name());
          }
        }
      }
    }

    return partialSolutions;
  }
}
