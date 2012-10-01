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

  protected SequentialGroup (ParaboxLayout<C> layout) {

    this(layout, Gap.RELATED);
  }

  protected SequentialGroup (ParaboxLayout<C> layout, Gap gap) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()));
  }

  protected SequentialGroup (ParaboxLayout<C> layout, double gap) {

    this(layout, gap, Justification.CENTER);
  }

  protected SequentialGroup (ParaboxLayout<C> layout, Justification justification) {

    this(layout, Gap.RELATED, justification);
  }

  protected SequentialGroup (ParaboxLayout<C> layout, Gap gap, Justification justification) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()), justification);
  }

  protected SequentialGroup (ParaboxLayout<C> layout, double gap, Justification justification) {

    super(layout);

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

  @Override
  public double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MINIMUM, tailor);
  }

  @Override
  public double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.PREFERRED, tailor);
  }

  @Override
  public double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MAXIMUM, tailor);
  }

  private synchronized double calculateMeasurement (Bias bias, TapeMeasure tapeMeasure, LayoutTailor tailor) {

    boolean first = true;
    double total = 0.0D;

    for (ParaboxElement<?> element : getElements()) {
      total += tapeMeasure.getMeasure(bias, element, tailor);
      if (!first) {
        total += gap;
      }
      first = false;
    }

    return total;
  }

  @Override
  public synchronized void doLayout (Bias bias, double containerPosition, double containerMeasurement, LayoutTailor tailor) {

    if (!getElements().isEmpty()) {

      double preferredContainerMeasure;

      if (containerMeasurement <= calculateMeasurement(bias, TapeMeasure.MINIMUM, tailor)) {

        double currentMeasure;
        double top = 0;

        for (ParaboxElement<?> element : getElements()) {

          tailor.applyLayout(bias, containerPosition + top, currentMeasure = element.getMinimumMeasurement(bias, tailor), element);
          top += currentMeasure + gap;
        }
      }
      else if (containerMeasurement <= (preferredContainerMeasure = calculateMeasurement(bias, TapeMeasure.PREFERRED, tailor))) {

        double[] preferredBiasedMeasurements = new double[getElements().size()];
        double[] fat = new double[getElements().size()];
        double currentMeasure;
        double totalShrink = 0;
        double totalFat = 0;
        double top = 0;
        int index;

        index = 0;
        for (ParaboxElement<?> element : getElements()) {
          totalShrink += element.getConstraint().getShrink();
          totalFat += (fat[index++] = (preferredBiasedMeasurements[index] = element.getPreferredMeasurement(bias, tailor)) - element.getMinimumMeasurement(bias, tailor));
        }

        index = 0;
        for (ParaboxElement<?> element : getElements()) {

          double totalRatio = (totalShrink + totalFat == 0) ? 0 : (element.getConstraint().getShrink() + fat[index]) / (totalShrink + totalFat);

          tailor.applyLayout(bias, containerPosition + top, currentMeasure = preferredBiasedMeasurements[index] - (totalRatio * (preferredContainerMeasure - containerMeasurement)), element);
          top += currentMeasure + gap;
        }
      }
      else {

        LinkedList<ReorderedElement> reorderedElements = new LinkedList<ReorderedElement>();
        double[] tentativeMeasurements = new double[getElements().size()];
        double[] maximumMeasurements = new double[getElements().size()];
        double unused = containerMeasurement - preferredContainerMeasure;
        double totalGrow = 0;
        int index = 0;

        for (ParaboxElement<?> element : getElements()) {

          double grow;

          if ((grow = element.getConstraint().getGrow()) > 0) {
            totalGrow += grow;
            reorderedElements.add(new ReorderedElement(element, index));
          }

          tentativeMeasurements[index] = element.getPreferredMeasurement(bias, tailor);
          maximumMeasurements[index++] = element.getMaximumMeasurement(bias, tailor);
        }

        if (!reorderedElements.isEmpty()) {
          do {

            Iterator<ReorderedElement> reorderedElementIter = reorderedElements.iterator();
            double used = 0;
            double spentGrowth = 0;

            while (reorderedElementIter.hasNext()) {

              ReorderedElement reorderedElement = reorderedElementIter.next();
              double currentUnused;
              double currentGrow;

              if ((tentativeMeasurements[reorderedElement.getOriginalIndex()] + (currentUnused = ((currentGrow = reorderedElement.getReorderedElement().getConstraint().getGrow()) / totalGrow * unused))) < maximumMeasurements[reorderedElement.getOriginalIndex()]) {
                used += currentUnused;
                tentativeMeasurements[reorderedElement.getOriginalIndex()] += currentUnused;
              }
              else {
                used += maximumMeasurements[reorderedElement.getOriginalIndex()] - tentativeMeasurements[reorderedElement.getOriginalIndex()];
                tentativeMeasurements[reorderedElement.getOriginalIndex()] = maximumMeasurements[reorderedElement.getOriginalIndex()];
                spentGrowth += currentGrow;
                reorderedElementIter.remove();
              }
            }

            unused -= used;
            totalGrow -= spentGrowth;

          } while ((!reorderedElements.isEmpty()) && (unused >= 1.0));
        }

        switch (getJustification()) {
          case FIRST:
            applyLayouts(bias, containerPosition, true, tentativeMeasurements, tailor);
            break;
          case LAST:
            applyLayouts(bias, containerPosition + containerMeasurement, false, tentativeMeasurements, tailor);
            break;
          case LEADING:
            if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
              applyLayouts(bias, containerPosition, true, tentativeMeasurements, tailor);
            }
            else {
              switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                case FIRST_TO_LAST:
                  applyLayouts(bias, containerPosition, true, tentativeMeasurements, tailor);
                  break;
                case LAST_TO_FIRST:
                  applyLayouts(bias, containerPosition + containerMeasurement, false, tentativeMeasurements, tailor);
                  break;
                default:
                  throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
              }
            }
            break;
          case TRAILING:
            if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().getBias())) {
              applyLayouts(bias, containerPosition + containerMeasurement, false, tentativeMeasurements, tailor);
            }
            else {
              switch (getLayout().getContainer().getPlatform().getOrientation().getFlow()) {
                case FIRST_TO_LAST:
                  applyLayouts(bias, containerPosition + containerMeasurement, false, tentativeMeasurements, tailor);
                  break;
                case LAST_TO_FIRST:
                  applyLayouts(bias, containerPosition, true, tentativeMeasurements, tailor);
                  break;
                default:
                  throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().getFlow().name());
              }
            }
            break;
          case CENTER:
            applyLayouts(bias, containerPosition + (unused / 2), true, tentativeMeasurements, tailor);
            break;
          default:
            throw new UnknownSwitchCaseException(getJustification().name());
        }
      }
    }
  }

  private void applyLayouts (Bias bias, double top, Boolean forward, double[] tentativeMeasurements, LayoutTailor tailor) {

    int index = 0;

    for (ParaboxElement<?> element : getElements()) {
      if (forward) {
        tailor.applyLayout(bias, top, tentativeMeasurements[index], element);
        top += tentativeMeasurements[index++] + gap;
      }
      else {
        tailor.applyLayout(bias, top -= tentativeMeasurements[index], tentativeMeasurements[index++], element);
        top -= gap;
      }
    }
  }
}
