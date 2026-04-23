/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import java.util.Iterator;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

// Lays out items one after another, in the Bias direction, with growth/shrink values also in the bias direction

/**
 * A {@link Box} that arranges its elements sequentially along the layout axis, separating them with a
 * configurable gap and distributing surplus or deficit space according to each element's grow/shrink
 * constraint and the box's {@link Justification} policy.
 */
public class SerialBox extends Box<SerialBox> {

  private Justification justification;
  private double gap;
  private boolean greedy;

  /**
   * Creates a serial box with the platform's unrelated gap and leading justification.
   *
   * @param layout the owning {@link ParaboxLayout}
   */
  protected SerialBox (ParaboxLayout layout) {

    this(layout, Gap.UNRELATED);
  }

  /**
   * Creates a serial box with the platform's unrelated gap, leading justification, and the specified greediness.
   *
   * @param layout the owning {@link ParaboxLayout}
   * @param greedy {@code true} to allow the box to consume all remaining space along the axis
   */
  protected SerialBox (ParaboxLayout layout, boolean greedy) {

    this(layout, Gap.UNRELATED, greedy);
  }

  /**
   * Creates a serial box with the specified predefined gap type and leading justification.
   *
   * @param layout the owning {@link ParaboxLayout}
   * @param gap    the predefined gap type used to space elements
   */
  protected SerialBox (ParaboxLayout layout, Gap gap) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()));
  }

  /**
   * Creates a serial box with the specified predefined gap type, leading justification, and greediness.
   *
   * @param layout the owning {@link ParaboxLayout}
   * @param gap    the predefined gap type used to space elements
   * @param greedy {@code true} to allow the box to consume all remaining space along the axis
   */
  protected SerialBox (ParaboxLayout layout, Gap gap, boolean greedy) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()), greedy);
  }

  /**
   * Creates a serial box with a fixed numeric gap and leading justification.
   *
   * @param layout the owning {@link ParaboxLayout}
   * @param gap    the fixed spacing between consecutive elements
   */
  protected SerialBox (ParaboxLayout layout, double gap) {

    this(layout, gap, Justification.LEADING);
  }

  /**
   * Creates a serial box with a fixed numeric gap, leading justification, and the specified greediness.
   *
   * @param layout the owning {@link ParaboxLayout}
   * @param gap    the fixed spacing between consecutive elements
   * @param greedy {@code true} to allow the box to consume all remaining space along the axis
   */
  protected SerialBox (ParaboxLayout layout, double gap, boolean greedy) {

    this(layout, gap, Justification.LEADING, greedy);
  }

  /**
   * Creates a serial box with the platform's unrelated gap and the specified justification.
   *
   * @param layout        the owning {@link ParaboxLayout}
   * @param justification the strategy for distributing surplus space
   */
  protected SerialBox (ParaboxLayout layout, Justification justification) {

    this(layout, Gap.UNRELATED, justification);
  }

  /**
   * Creates a serial box with the platform's unrelated gap, the specified justification, and greediness.
   *
   * @param layout        the owning {@link ParaboxLayout}
   * @param justification the strategy for distributing surplus space
   * @param greedy        {@code true} to allow the box to consume all remaining space along the axis
   */
  protected SerialBox (ParaboxLayout layout, Justification justification, boolean greedy) {

    this(layout, Gap.UNRELATED, justification, greedy);
  }

  /**
   * Creates a serial box with a predefined gap type and the specified justification.
   *
   * @param layout        the owning {@link ParaboxLayout}
   * @param gap           the predefined gap type used to space elements
   * @param justification the strategy for distributing surplus space
   */
  protected SerialBox (ParaboxLayout layout, Gap gap, Justification justification) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()), justification);
  }

  /**
   * Creates a serial box with a predefined gap type, the specified justification, and greediness.
   *
   * @param layout        the owning {@link ParaboxLayout}
   * @param gap           the predefined gap type used to space elements
   * @param justification the strategy for distributing surplus space
   * @param greedy        {@code true} to allow the box to consume all remaining space along the axis
   */
  protected SerialBox (ParaboxLayout layout, Gap gap, Justification justification, boolean greedy) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()), justification, greedy);
  }

  /**
   * Creates a serial box with a fixed numeric gap and the specified justification.
   *
   * @param layout        the owning {@link ParaboxLayout}
   * @param gap           the fixed spacing between consecutive elements
   * @param justification the strategy for distributing surplus space
   */
  protected SerialBox (ParaboxLayout layout, double gap, Justification justification) {

    this(layout, gap, justification, false);
  }

  /**
   * Creates a serial box with a fixed numeric gap, the specified justification, and greediness.
   *
   * @param layout        the owning {@link ParaboxLayout}
   * @param gap           the fixed spacing between consecutive elements
   * @param justification the strategy for distributing surplus space
   * @param greedy        {@code true} to allow the box to consume all remaining space along the axis
   */
  protected SerialBox (ParaboxLayout layout, double gap, Justification justification, boolean greedy) {

    super(SerialBox.class, layout);

    this.justification = justification;
    this.gap = gap;
    this.greedy = greedy;
  }

  /**
   * Returns the fixed spacing inserted between consecutive elements.
   *
   * @return the gap value in platform units
   */
  public double getGap () {

    return gap;
  }

  /**
   * Sets the gap using a predefined gap type resolved against the platform.
   *
   * @param gap the predefined gap type to resolve and apply
   * @return this box for method chaining
   */
  public SerialBox setGap (Gap gap) {

    return setGap(gap.getGap(getLayout().getContainer().getPlatform()));
  }

  /**
   * Sets the fixed spacing inserted between consecutive elements.
   *
   * @param gap the spacing value in platform units
   * @return this box for method chaining
   */
  public SerialBox setGap (double gap) {

    this.gap = gap;

    return this;
  }

  /**
   * Returns the justification strategy applied when the total preferred size is less than the available space.
   *
   * @return the current justification
   */
  public Justification getJustification () {

    return justification;
  }

  /**
   * Sets the justification strategy for distributing surplus space.
   *
   * @param justification the justification to apply
   * @return this box for method chaining
   */
  public SerialBox setJustification (Justification justification) {

    this.justification = justification;

    return this;
  }

  /**
   * Returns whether this box reports an effectively unlimited maximum measurement, consuming all available space.
   *
   * @return {@code true} if this box is greedy
   */
  public boolean isGreedy () {

    return greedy;
  }

  /**
   * Sets whether this box reports an effectively unlimited maximum measurement.
   *
   * @param greedy {@code true} to make the box consume all available space
   * @return this box for method chaining
   */
  public SerialBox setGreedy (boolean greedy) {

    this.greedy = greedy;

    return this;
  }

  /**
   * Returns the minimum size this box requires along the given axis, summing each element's minimum
   * measurement and the gaps between them.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching; may be {@code null}
   * @return the total minimum size along the axis
   */
  @Override
  public double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MINIMUM, tailor);
  }

  /**
   * Returns the preferred size this box requests along the given axis, summing each element's preferred
   * measurement and the gaps between them.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching; may be {@code null}
   * @return the total preferred size along the axis
   */
  @Override
  public double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.PREFERRED, tailor);
  }

  /**
   * Returns the maximum size this box can occupy along the given axis; returns {@link Integer#MAX_VALUE}
   * if this box is greedy, otherwise sums each element's maximum measurement and the gaps between them.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching; may be {@code null}
   * @return the total maximum size along the axis
   */
  @Override
  public double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return greedy ? Integer.MAX_VALUE : calculateMeasurement(bias, TapeMeasure.MAXIMUM, tailor);
  }

  /**
   * Sums the specified measurement type for all elements plus the inter-element gaps.
   *
   * @param bias        the axis of measurement
   * @param tapeMeasure the category of measurement to retrieve from each element
   * @param tailor      the layout tailor for recursive caching; may be {@code null}
   * @return the aggregate measurement including all gaps
   */
  private synchronized double calculateMeasurement (Bias bias, TapeMeasure tapeMeasure, LayoutTailor tailor) {

    double total = 0.0D;

    if (!getElements().isEmpty()) {

      boolean first = true;

      for (ParaboxElement<?> element : getElements()) {
        total += tapeMeasure.getMeasure(bias, element, tailor);
        if (!first) {
          total += gap;
        }
        first = false;
      }
    }

    return total;
  }

  /**
   * Lays out elements sequentially along the given axis, shrinking, stretching, or justifying them based
   * on how the container measurement compares to the aggregate minimum, preferred, and maximum measurements.
   *
   * @param bias                 the axis along which to position elements
   * @param containerPosition    the starting offset along the axis
   * @param containerMeasurement the total space available along the axis
   * @param tailor               the {@link LayoutTailor} coordinating the full layout pass
   */
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
      } else if (containerMeasurement <= (preferredContainerMeasure = calculateMeasurement(bias, TapeMeasure.PREFERRED, tailor))) {

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
          totalFat += (fat[index] = (preferredBiasedMeasurements[index++] = element.getPreferredMeasurement(bias, tailor)) - element.getMinimumMeasurement(bias, tailor));
        }

        index = 0;
        for (ParaboxElement<?> element : getElements()) {

          double totalRatio = (totalShrink + totalFat == 0) ? 0 : (element.getConstraint().getShrink() + fat[index]) / (totalShrink + totalFat);

          tailor.applyLayout(bias, containerPosition + top, currentMeasure = preferredBiasedMeasurements[index++] - (totalRatio * (preferredContainerMeasure - containerMeasurement)), element);
          top += currentMeasure + gap;
        }
      } else {

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

              if ((tentativeMeasurements[reorderedElement.originalIndex()] + (currentUnused = ((currentGrow = reorderedElement.reorderedElement().getConstraint().getGrow()) / totalGrow * unused))) < maximumMeasurements[reorderedElement.originalIndex()]) {
                used += currentUnused;
                tentativeMeasurements[reorderedElement.originalIndex()] += currentUnused;
              } else {
                used += maximumMeasurements[reorderedElement.originalIndex()] - tentativeMeasurements[reorderedElement.originalIndex()];
                tentativeMeasurements[reorderedElement.originalIndex()] = maximumMeasurements[reorderedElement.originalIndex()];
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
            if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().bias())) {
              applyLayouts(bias, containerPosition, true, tentativeMeasurements, tailor);
            } else {
              switch (getLayout().getContainer().getPlatform().getOrientation().flow()) {
                case FIRST_TO_LAST:
                  applyLayouts(bias, containerPosition, true, tentativeMeasurements, tailor);
                  break;
                case LAST_TO_FIRST:
                  applyLayouts(bias, containerPosition + containerMeasurement, false, tentativeMeasurements, tailor);
                  break;
                default:
                  throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().flow().name());
              }
            }
            break;
          case TRAILING:
            if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().bias())) {
              applyLayouts(bias, containerPosition + containerMeasurement, false, tentativeMeasurements, tailor);
            } else {
              switch (getLayout().getContainer().getPlatform().getOrientation().flow()) {
                case FIRST_TO_LAST:
                  applyLayouts(bias, containerPosition + containerMeasurement, false, tentativeMeasurements, tailor);
                  break;
                case LAST_TO_FIRST:
                  applyLayouts(bias, containerPosition, true, tentativeMeasurements, tailor);
                  break;
                default:
                  throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().flow().name());
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

  /**
   * Places each element along the axis using precomputed tentative measurements, either advancing forward
   * from the starting position or retreating backward from an end position.
   *
   * @param bias                  the axis along which elements are placed
   * @param top                   the starting (forward) or ending (reverse) offset along the axis
   * @param forward               {@code true} to iterate elements in order from {@code top}; {@code false} to iterate in reverse
   * @param tentativeMeasurements the computed size to assign to each element, indexed by element order
   * @param tailor                the {@link LayoutTailor} coordinating the full layout pass
   */
  private void applyLayouts (Bias bias, double top, Boolean forward, double[] tentativeMeasurements, LayoutTailor tailor) {

    int index = 0;

    for (ParaboxElement<?> element : getElements()) {
      if (forward) {
        tailor.applyLayout(bias, top, tentativeMeasurements[index], element);
        top += tentativeMeasurements[index++] + gap;
      } else {
        tailor.applyLayout(bias, top -= tentativeMeasurements[index], tentativeMeasurements[index++], element);
        top -= gap;
      }
    }
  }
}
