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
 * Box that lays out elements sequentially along the specified {@link Bias}, honoring grow/shrink constraints
 * and optional gaps and justification settings.
 */
public class SerialBox extends Box<SerialBox> {

  private Justification justification;
  private double gap;
  private boolean greedy;

  /**
   * Creates a serial box with default unrelated gap and leading justification.
   *
   * @param layout the owning layout
   */
  protected SerialBox (ParaboxLayout layout) {

    this(layout, Gap.UNRELATED);
  }

  /**
   * Creates a serial box with default gap and a greediness flag.
   *
   * @param layout the owning layout
   * @param greedy whether the box may grow indefinitely
   */
  protected SerialBox (ParaboxLayout layout, boolean greedy) {

    this(layout, Gap.UNRELATED, greedy);
  }

  /**
   * Creates a serial box with the given gap.
   *
   * @param layout the owning layout
   * @param gap    the gap type between elements
   */
  protected SerialBox (ParaboxLayout layout, Gap gap) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()));
  }

  /**
   * Creates a serial box with a gap and greediness flag.
   *
   * @param layout the owning layout
   * @param gap    the gap type between elements
   * @param greedy whether the box may grow indefinitely
   */
  protected SerialBox (ParaboxLayout layout, Gap gap, boolean greedy) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()), greedy);
  }

  /**
   * Creates a serial box with a fixed gap.
   *
   * @param layout the owning layout
   * @param gap    the fixed gap value
   */
  protected SerialBox (ParaboxLayout layout, double gap) {

    this(layout, gap, Justification.LEADING);
  }

  /**
   * Creates a serial box with a fixed gap and greediness flag.
   *
   * @param layout the owning layout
   * @param gap    the fixed gap value
   * @param greedy whether the box may grow indefinitely
   */
  protected SerialBox (ParaboxLayout layout, double gap, boolean greedy) {

    this(layout, gap, Justification.LEADING, greedy);
  }

  /**
   * Creates a serial box with justification and default gap.
   *
   * @param layout        the owning layout
   * @param justification justification used when distributing extra space
   */
  protected SerialBox (ParaboxLayout layout, Justification justification) {

    this(layout, Gap.UNRELATED, justification);
  }

  /**
   * Creates a serial box with justification, default gap, and greediness flag.
   *
   * @param layout        the owning layout
   * @param justification justification used when distributing extra space
   * @param greedy        whether the box may grow indefinitely
   */
  protected SerialBox (ParaboxLayout layout, Justification justification, boolean greedy) {

    this(layout, Gap.UNRELATED, justification, greedy);
  }

  /**
   * Creates a serial box with a gap type and justification.
   *
   * @param layout        the owning layout
   * @param gap           the gap type between elements
   * @param justification justification used when distributing extra space
   */
  protected SerialBox (ParaboxLayout layout, Gap gap, Justification justification) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()), justification);
  }

  /**
   * Creates a serial box with a gap type, justification, and greediness flag.
   *
   * @param layout        the owning layout
   * @param gap           the gap type between elements
   * @param justification justification used when distributing extra space
   * @param greedy        whether the box may grow indefinitely
   */
  protected SerialBox (ParaboxLayout layout, Gap gap, Justification justification, boolean greedy) {

    this(layout, gap.getGap(layout.getContainer().getPlatform()), justification, greedy);
  }

  /**
   * Creates a serial box with a fixed gap and justification.
   *
   * @param layout        the owning layout
   * @param gap           the fixed gap value
   * @param justification justification used when distributing extra space
   */
  protected SerialBox (ParaboxLayout layout, double gap, Justification justification) {

    this(layout, gap, justification, false);
  }

  /**
   * Creates a serial box with a fixed gap, justification, and greediness flag.
   *
   * @param layout        the owning layout
   * @param gap           the fixed gap value
   * @param justification justification used when distributing extra space
   * @param greedy        whether the box may grow indefinitely
   */
  protected SerialBox (ParaboxLayout layout, double gap, Justification justification, boolean greedy) {

    super(SerialBox.class, layout);

    this.justification = justification;
    this.gap = gap;
    this.greedy = greedy;
  }

  /**
   * @return the gap between elements
   */
  public double getGap () {

    return gap;
  }

  /**
   * Sets the gap using a predefined gap type.
   *
   * @param gap the gap type
   * @return this box for chaining
   */
  public SerialBox setGap (Gap gap) {

    return setGap(gap.getGap(getLayout().getContainer().getPlatform()));
  }

  /**
   * Sets the fixed gap value.
   *
   * @param gap the fixed gap
   * @return this box for chaining
   */
  public SerialBox setGap (double gap) {

    this.gap = gap;

    return this;
  }

  /**
   * @return the justification used when distributing extra space
   */
  public Justification getJustification () {

    return justification;
  }

  /**
   * Sets the justification strategy.
   *
   * @param justification the justification to use
   * @return this box for chaining
   */
  public SerialBox setJustification (Justification justification) {

    this.justification = justification;

    return this;
  }

  /**
   * @return whether this box may grow without bound
   */
  public boolean isGreedy () {

    return greedy;
  }

  /**
   * Sets whether this box may grow without bound.
   *
   * @param greedy {@code true} to allow unbounded growth
   * @return this box for chaining
   */
  public SerialBox setGreedy (boolean greedy) {

    this.greedy = greedy;

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MINIMUM, tailor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.PREFERRED, tailor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return greedy ? Integer.MAX_VALUE : calculateMeasurement(bias, TapeMeasure.MAXIMUM, tailor);
  }

  /**
   * Computes the aggregate measurement of all children along the specified axis, including gaps.
   *
   * @param bias        the axis of measurement
   * @param tapeMeasure which measurement to compute
   * @param tailor      layout tailor for caching
   * @return the total measurement
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
   * {@inheritDoc}
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
   * Applies layouts to each element using precomputed tentative measurements and justification direction.
   *
   * @param bias                  the axis of layout
   * @param top                   the starting position along the axis
   * @param forward               {@code true} to lay out forward, {@code false} to lay out in reverse
   * @param tentativeMeasurements the measurement allocated to each element
   * @param tailor                layout tailor coordinating multi-axis layout
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
