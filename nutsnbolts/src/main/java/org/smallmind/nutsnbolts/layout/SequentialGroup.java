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
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class SequentialGroup<C> extends Group<C, SequentialGroup> {

  private Justification justification;
  private double gap;

  public SequentialGroup (ParaboxLayout<C> layout, Bias bias) {

    this(layout, bias, Gap.RELATED);
  }

  public SequentialGroup (ParaboxLayout<C> layout, Bias bias, Gap gap) {

    this(layout, bias, gap.getGap(layout.getContainer().getPlatform()));
  }

  public SequentialGroup (ParaboxLayout<C> layout, Bias bias, double gap) {

    this(layout, bias, gap, Justification.CENTER);
  }

  public SequentialGroup (ParaboxLayout<C> layout, Bias bias, Justification justification) {

    this(layout, bias, Gap.RELATED, justification);
  }

  public SequentialGroup (ParaboxLayout<C> layout, Bias bias, Gap gap, Justification justification) {

    this(layout, bias, gap.getGap(layout.getContainer().getPlatform()), justification);
  }

  public SequentialGroup (ParaboxLayout<C> layout, Bias bias, double gap, Justification justification) {

    super(layout, bias);

    this.justification = justification;
    this.gap = gap;
  }

  public double getGap () {

    return gap;
  }

  public SequentialGroup setGap (Gap gap) {

    return setGap(gap.getGap(getLayout().getContainer().getPlatform()));
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

  public double calculateMinimumMeasurement () {

    return calculateMeasurement(TapeMeasure.MINIMUM);
  }

  public double calculatePreferredMeasurement () {

    return calculateMeasurement(TapeMeasure.PREFERRED);
  }

  public double calculateMaximumMeasurement () {

    return calculateMeasurement(TapeMeasure.MAXIMUM);
  }

  private synchronized double calculateMeasurement (TapeMeasure tapeMeasure) {

    boolean first = true;
    double total = 0.0D;

    for (ParaboxElement<?> element : getElements()) {
      total += tapeMeasure.getMeasure(getBias(), element);
      if (!first) {
        total += gap;
      }
      first = false;
    }

    return total;
  }

  @Override
  public synchronized void doLayout (double containerPosition, double containerMeasurement) {

    if (!getElements().isEmpty()) {

      double preferredContainerMeasure;

      if (containerMeasurement <= calculateMeasurement(TapeMeasure.MINIMUM)) {

        double currentMeasure;
        double top = 0;

        for (ParaboxElement<?> element : getElements()) {

          element.applyLayout(getBias(), containerPosition + top, currentMeasure = getBias().getMinimumMeasurement(element));
          top += currentMeasure + gap;
        }
      }
      else if (containerMeasurement <= (preferredContainerMeasure = calculateMeasurement(TapeMeasure.PREFERRED))) {

        double[] preferredBiasedMeasurements = new double[getElements().size()];
        double[] fat = new double[getElements().size()];
        double currentMeasure;
        double totalShrink = 0;
        double totalFat = 0;
        double top = 0;
        int index;

        index = 0;
        for (ParaboxElement<?> element : getElements()) {
          totalShrink += getBias().getShrink(element);
          totalFat += (fat[index++] = (preferredBiasedMeasurements[index] = getBias().getPreferredMeasurement(element)) - getBias().getMinimumMeasurement(element));
        }

        index = 0;
        for (ParaboxElement<?> element : getElements()) {

          double totalRatio = (totalShrink + totalFat == 0) ? 0 : (getBias().getShrink(element) + fat[index]) / (totalShrink + totalFat);

          element.applyLayout(getBias(), containerPosition + top, currentMeasure = preferredBiasedMeasurements[index] - (totalRatio * (preferredContainerMeasure - containerMeasurement))));
          top += currentMeasure + gap;
        }
      }
      else {

        PartialSolution[] partialSolutions = new PartialSolution[getElements().size()];
        LinkedList<ReorderedElement> reorderedElements = new LinkedList<ReorderedElement>();
        double[] maximumBiasedMeasurements = new double[getElements().size()];
        double unused = containerMeasurement - preferredContainerMeasure;
        double totalGrow = 0;
        int index;

        index = 0;
        for (ParaboxElement<?> element : getElements()) {

          double grow;

          if ((grow = getBias().getGrow(element)) > 0) {
            totalGrow += grow;
            reorderedElements.add(new ReorderedElement(element, index));
          }

          partialSolutions[index] = new PartialSolution(containerPosition, getBias().getPreferredMeasurement(element));
          maximumBiasedMeasurements[index++] = getBias().getMaximumMeasurement(element);
        }

        if (!reorderedElements.isEmpty()) {
          do {

            Iterator<ReorderedElement> reorderedElementIter = reorderedElements.iterator();
            double used = 0;
            double spentGrowth = 0;

            while (reorderedElementIter.hasNext()) {

              ReorderedElement reorderedElement = reorderedElementIter.next();
              double increasedMeasurement;
              double currentUnused;
              double currentGrow;

              if ((increasedMeasurement = partialSolutions[reorderedElement.getOriginalIndex()].getMeasurement() + (currentUnused = (((currentGrow = getBias().getGrow(reorderedElement.getReorderedElement())) / totalGrow) * unused))) < maximumBiasedMeasurements[reorderedElement.getOriginalIndex()]) {
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

        switch (getJustification()) {
          case FIRST:
            adjustPartialPositions(containerPosition, true, partialSolutions);
            break;
          case LAST:
            adjustPartialPositions(containerPosition + containerMeasurement, false, partialSolutions);
            break;
          case LEADING:
            if (!getBias().equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
              adjustPartialPositions(containerPosition, true, partialSolutions);
            }
            else {
              switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                case FIRST_TO_LAST:
                  adjustPartialPositions(containerPosition, true, partialSolutions);
                  break;
                case LAST_TO_FIRST:
                  adjustPartialPositions(containerPosition + containerMeasurement, false, partialSolutions);
                  break;
                default:
                  throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
              }
            }
            break;
          case TRAILING:
            if (!getBias().equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
              adjustPartialPositions(containerPosition + containerMeasurement, false, partialSolutions);
            }
            else {
              switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                case FIRST_TO_LAST:
                  adjustPartialPositions(containerPosition + containerMeasurement, false, partialSolutions);
                  break;
                case LAST_TO_FIRST:
                  adjustPartialPositions(containerPosition, true, partialSolutions);
                  break;
                default:
                  throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
              }
            }
            break;
          case CENTER:
            adjustPartialPositions(containerPosition + (unused / 2), true, partialSolutions);
            break;
          default:
            throw new UnknownSwitchCaseException(getJustification().name());
        }

        index = 0;
        for (ParaboxElement<?> element : getElements()) {
          element.applyLayout(getBias(), partialSolutions[index].getPosition(), partialSolutions[index++].getMeasurement());
        }
      }
    }
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
