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

public class ParaboxLayout<E extends ParaboxElement<?>> {

  private ParaboxContainer container;
  private Bias bias;
  private Alignment biasedAlignment;
  private Alignment unbiasedAlignment;
  private Double minimumUnbiasedMeasurement;
  private Double preferredUnbiasedMeasurement;
  private Double maximumUnbiasedMeasurement;
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

  public Double getMinimumUnbiasedMeasurement () {

    return minimumUnbiasedMeasurement;
  }

  public ParaboxLayout<E> setMinimumUnbiasedMeasurement (Double minimumUnbiasedMeasurement) {

    this.minimumUnbiasedMeasurement = minimumUnbiasedMeasurement;

    return this;
  }

  public Double getPreferredUnbiasedMeasurement () {

    return preferredUnbiasedMeasurement;
  }

  public ParaboxLayout<E> setPreferredUnbiasedMeasurement (Double preferredUnbiasedMeasurement) {

    this.preferredUnbiasedMeasurement = preferredUnbiasedMeasurement;

    return this;
  }

  public Double getMaximumUnbiasedMeasurement () {

    return maximumUnbiasedMeasurement;
  }

  public ParaboxLayout<E> setMaximumUnbiasedMeasurement (Double maximumUnbiasedMeasurement) {

    this.maximumUnbiasedMeasurement = maximumUnbiasedMeasurement;

    return this;
  }

  public Pair calculateMinimumContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculateMinimumBiasedContainerMeasurement(elements), calculateMinimumUnbiasedContainerMeasurement(elements));
  }

  private double calculateMinimumBiasedContainerMeasurement (List<E> elements) {

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

  private double calculateMinimumUnbiasedContainerMeasurement (List<E> elements) {

    double maxAscent = 0;
    double maxDescent = 0;

    for (E element : elements) {

      double currentMeasurement = (minimumUnbiasedMeasurement != null) ? minimumUnbiasedMeasurement : getBias().getMinimumUnbiasedMeasurement(element);
      double currentAscent = (!unbiasedAlignment.equals(Alignment.BASELINE)) ? currentMeasurement : element.getBaseline(bias, currentMeasurement);
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

  public Pair calculatePreferredContainerSize (List<E> elements) {

    return getBias().getBiasedPair(calculatePreferredBiasedContainerMeasurement(elements), calculatePreferredUnbiasedContainerMeasurement(elements));
  }

  private double calculatePreferredBiasedContainerMeasurement (List<E> elements) {

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

  private double calculatePreferredUnbiasedContainerMeasurement (List<E> elements) {

    double maxAscent = 0;
    double maxDescent = 0;

    for (E element : elements) {

      double currentMeasurement = (preferredUnbiasedMeasurement != null) ? preferredUnbiasedMeasurement : getBias().getPreferredUnbiasedMeasurement(element);
      double currentAscent = (!unbiasedAlignment.equals(Alignment.BASELINE)) ? currentMeasurement : element.getBaseline(bias, currentMeasurement);
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

  private double calculateMinimumBiasedContainerMeasurement (List<E> elements) {

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

  private double calculateMinimumUnbiasedContainerMeasurement (List<E> elements) {

    double maxAscent = 0;
    double maxDescent = 0;

    for (E element : elements) {

      double currentMeasurement = (minimumUnbiasedMeasurement != null) ? minimumUnbiasedMeasurement : getBias().getMinimumUnbiasedMeasurement(element);
      double currentAscent = (!unbiasedAlignment.equals(Alignment.BASELINE)) ? currentMeasurement : element.getBaseline(bias, currentMeasurement);
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

  public void doLayout (double width, double height, List<E> elements) {

    PartialSolution[] biasedPartialSolutions = doBiasedLayout(bias.getBiasedMeasurement(width, height), elements);
    PartialSolution[] unbiasedPartialSolutions = doUnbiasedLayout(bias.getUnbiasedMeasurement(width, height), elements);
    int index = 0;

    for (E element : elements) {

    }
  }

  private PartialSolution[] doBiasedLayout (double biasedContainerMeasure, List<E> elements) {

    PartialSolution[] partialSolutions = new PartialSolution[(elements == null) ? 0 : elements.size()];

    if (elements != null) {

      double preferredBiasedContainerMeasure;

      if (biasedContainerMeasure <= calculateMinimumBiasedContainerMeasurement(elements)) {

        double currentMeasure;
        double top = 0;
        int index = 0;

        for (E element : elements) {
          partialSolutions[index++] = new PartialSolution(top, currentMeasure = bias.getMinimumBiasedMeasurement(element));
          top += currentMeasure + gap;
        }
      }
      else if (biasedContainerMeasure <= (preferredBiasedContainerMeasure = calculatePreferredBiasedContainerMeasurement(elements))) {

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

  private PartialSolution[] doUnbiasedLayout (double unbiasedContainerMeasurement, List<E> elements) {

    PartialSolution[] partialSolutions = new PartialSolution[(elements == null) ? 0 : elements.size()];

    if (elements != null) {

      BaselineCalculations<E> baselineCalculations = (unbiasedAlignment.equals(Alignment.BASELINE)) ? new BaselineCalculations<E>(bias, maximumUnbiasedMeasurement, unbiasedContainerMeasurement, elements) : null;
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
          switch (unbiasedAlignment) {
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
              throw new UnknownSwitchCaseException(unbiasedAlignment.name());
          }
        }
      }
    }

    return partialSolutions;
  }
}
