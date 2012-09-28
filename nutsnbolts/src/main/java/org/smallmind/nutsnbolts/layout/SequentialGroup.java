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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class SequentialGroup extends Group<SequentialGroup> {

  private Justification justification;
  private double gap;

  public SequentialGroup (Bias bias) {

    this(bias, Gap.RELATED);
  }

  public SequentialGroup (Bias bias, Gap gap) {

    this(bias, gap.getGap(container.getPlatform()));
  }

  public SequentialGroup (Bias bias, double gap) {

    this(bias, gap, Justification.CENTER);
  }

  public SequentialGroup (Bias bias, Justification justification) {

    this(bias, Gap.RELATED, justification);
  }

  public SequentialGroup (Bias bias, Gap gap, Justification justification) {

    this(bias, gap.getGap(container.getPlatform()), justification);
  }

  public SequentialGroup (Bias bias, double gap, Justification justification) {

    super(bias);

    this.justification = justification;
    this.gap = gap;
  }

  public double getGap () {

    return gap;
  }

  public SequentialGroup setGap (Gap gap) {

    return setGap(gap.getGap(container.getPlatform()));
  }

  public SequentialGroup setGap (double gap) {

    this.gap = gap;

    return this;
  }

  public Justification getJustification () {

    return justification;
  }

  public SequentialGroup setJustification (Justification justification) {

    this.justification = justification;

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

  private double calculateContainerMeasurement (TapeMeasure tapeMeasure, List<E> elements) {

    boolean first = true;
    double total = 0.0D;

    for (E element : elements) {
      total += tapeMeasure.getBiasedMeasure(bias, element);
      if (!first) {
        total += gap;
      }
      first = false;
    }

    return total;
  }

  private PartialSolution[] doLayout (double biasedContainerMeasure, List<E> elements) {

    PartialSolution[] partialSolutions = new PartialSolution[(elements == null) ? 0 : elements.size()];

    if (elements != null) {

      double preferredBiasedContainerMeasure;

      if (biasedContainerMeasure <= calculateBiasedContainerMeasurement(TapeMeasure.MINIMUM, elements)) {

        double currentMeasure;
        double top = 0;
        int index = 0;

        for (E element : elements) {
          partialSolutions[index++] = new PartialSolution(top, currentMeasure = bias.getMinimumBiasedMeasurement(element));
          top += currentMeasure + gap;
        }
      }
      else if (biasedContainerMeasure <= (preferredBiasedContainerMeasure = calculateBiasedContainerMeasurement(TapeMeasure.PREFERRED, elements))) {

        double[] preferredBiasedMeasurements = new double[elements.size()];
        double[] fat = new double[elements.size()];
        double currentMeasure;
        double totalShrink = 0;
        double totalFat = 0;
        double top = 0;
        int index;

        index = 0;
        for (E element : elements) {
          totalShrink += bias.getBiasedShrink(element);
          totalFat += (fat[index++] = (preferredBiasedMeasurements[index] = bias.getPreferredBiasedMeasurement(element)) - bias.getMinimumBiasedMeasurement(element));
        }

        index = 0;
        for (E element : elements) {

          double totalRatio = (totalShrink + totalFat == 0) ? 0 : (bias.getBiasedShrink(element) + fat[index]) / (totalShrink + totalFat);

          partialSolutions[index++] = new PartialSolution(top, currentMeasure = preferredBiasedMeasurements[index] - (totalRatio * (preferredBiasedContainerMeasure - biasedContainerMeasure)));
          top += currentMeasure + gap;
        }
      }
      else {

        LinkedList<ReorderedElement<E>> reorderedElements = new LinkedList<ReorderedElement<E>>();
        double[] maximumBiasedMeasurements = new double[elements.size()];
        double unused = biasedContainerMeasure - preferredBiasedContainerMeasure;
        double totalGrow = 0;
        int index = 0;

        for (E element : elements) {

          double grow;

          if ((grow = bias.getBiasedGrow(element)) > 0) {
            totalGrow += grow;
            reorderedElements.add(new ReorderedElement<E>(element, index));
          }

          partialSolutions[index] = new PartialSolution(0, bias.getPreferredBiasedMeasurement(element));
          maximumBiasedMeasurements[index++] = bias.getMaximumBiasedMeasurement(element);
        }

        if (!reorderedElements.isEmpty()) {
          do {

            Iterator<ReorderedElement<E>> reorderedElementIter = reorderedElements.iterator();
            double used = 0;
            double spentGrowth = 0;

            while (reorderedElementIter.hasNext()) {

              ReorderedElement<E> reorderedElement = reorderedElementIter.next();
              double increasedMeasurement;
              double currentUnused;
              double currentGrow;

              if ((increasedMeasurement = partialSolutions[reorderedElement.getOriginalIndex()].getMeasurement() + (currentUnused = (((currentGrow = bias.getBiasedGrow(reorderedElement.getReorderedElement())) / totalGrow) * unused))) < maximumBiasedMeasurements[reorderedElement.getOriginalIndex()]) {
                used += currentUnused;
                partialSolutions[reorderedElement.getOriginalIndex()].setMeasurement(increasedMeasurement);
              }
              else {
                used += maximumBiasedMeasurements[reorderedElement.getOriginalIndex()] - partialSolutions[reorderedElement.getOriginalIndex()].getMeasurement();
                spentGrowth += currentGrow;
                partialSolutions[reorderedElement.getOriginalIndex()].setMeasurement(maximumBiasedMeasurements[reorderedElement.getOriginalIndex()]);
                reorderedElementIter.remove();
              }
            }

            unused -= used;
            totalGrow -= spentGrowth;

          } while ((!reorderedElements.isEmpty()) && (unused >= 1.0));
        }

        switch (biasedAlignment) {
          case FIRST:
            adjustPartialPositions(0, true, partialSolutions);
            break;
          case LAST:
            adjustPartialPositions(biasedContainerMeasure, false, partialSolutions);
            break;
          case LEADING:
            if (!bias.equals(container.getPlatform().getOrientation().getBias())) {
              adjustPartialPositions(0, true, partialSolutions);
            }
            else {
              switch (container.getPlatform().getOrientation().getFlow()) {
                case FIRST_TO_LAST:
                  adjustPartialPositions(0, true, partialSolutions);
                  break;
                case LAST_TO_FIRST:
                  adjustPartialPositions(biasedContainerMeasure, false, partialSolutions);
                  break;
                default:
                  throw new UnknownSwitchCaseException(container.getPlatform().getOrientation().getFlow().name());
              }
            }
            break;
          case TRAILING:
            if (!bias.equals(container.getPlatform().getOrientation().getBias())) {
              adjustPartialPositions(biasedContainerMeasure, false, partialSolutions);
            }
            else {
              switch (container.getPlatform().getOrientation().getFlow()) {
                case FIRST_TO_LAST:
                  adjustPartialPositions(biasedContainerMeasure, false, partialSolutions);
                  break;
                case LAST_TO_FIRST:
                  adjustPartialPositions(0, true, partialSolutions);
                  break;
                default:
                  throw new UnknownSwitchCaseException(container.getPlatform().getOrientation().getFlow().name());
              }
            }
            break;
          case CENTER:
            adjustPartialPositions(unused / 2, true, partialSolutions);
            break;
          case BASELINE:
            throw new UnsupportedOperationException("Attempt to use BASELINE alignment in the biased orientation");
          default:
            throw new UnknownSwitchCaseException(biasedAlignment.name());
        }
      }
    }

    return partialSolutions;
  }

  private void adjustPartialPositions (double top, Boolean forward, PartialSolution[] partialSolutions) {

    for (PartialSolution partialSolution : partialSolutions) {
      if (forward) {
        partialSolution.setPosition(top);
        top += partialSolution.getMeasurement() + gap;
      }
      else {
        partialSolution.setPosition(top -= partialSolution.getMeasurement());
        top -= gap;
      }
    }
  }
}
