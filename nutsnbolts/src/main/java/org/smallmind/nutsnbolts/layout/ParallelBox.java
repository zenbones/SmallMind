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

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * A {@link Box} that places all its elements at the same position along the layout axis, sizing each
 * independently along that axis and aligning them according to the configured {@link Alignment}.
 * Optional measurement overrides clamp all elements to a uniform minimum, preferred, or maximum size.
 */
public class ParallelBox extends Box<ParallelBox> {

  private Alignment alignment;
  private Double minimumOverrideMeasurement;
  private Double preferredOverrideMeasurement;
  private Double maximumOverrideMeasurement;

  // Lays out items one after another, in the direction *opposite* to the Bias, but with growth/shrink values still in the bias direction

  /**
   * Creates a parallel box with {@link Alignment#LEADING} as the default alignment.
   *
   * @param layout the owning {@link ParaboxLayout}
   */
  protected ParallelBox (ParaboxLayout layout) {

    this(layout, Alignment.LEADING);
  }

  /**
   * Creates a parallel box with the specified alignment.
   *
   * @param layout    the owning {@link ParaboxLayout}
   * @param alignment the alignment applied to each element within the available space
   */
  protected ParallelBox (ParaboxLayout layout, Alignment alignment) {

    super(ParallelBox.class, layout);

    this.alignment = alignment;
  }

  /**
   * Returns the alignment used to position each element within the box's available space.
   *
   * @return the current alignment
   */
  public Alignment getAlignment () {

    return alignment;
  }

  /**
   * Sets the alignment used to position each element within the box's available space.
   *
   * @param alignment the alignment to apply
   * @return this box for method chaining
   */
  public ParallelBox setAlignment (Alignment alignment) {

    this.alignment = alignment;

    return this;
  }

  /**
   * Returns the minimum measurement override applied to all children, or {@code null} if each child
   * reports its own minimum.
   *
   * @return the minimum override, or {@code null}
   */
  public Double getMinimumOverrideMeasurement () {

    return minimumOverrideMeasurement;
  }

  /**
   * Sets an override that clamps every child's minimum measurement to the given value.
   *
   * @param minimumOverrideMeasurement the override value, or {@code null} to use each child's own minimum
   * @return this box for method chaining
   */
  public ParallelBox setMinimumOverrideMeasurement (Double minimumOverrideMeasurement) {

    this.minimumOverrideMeasurement = minimumOverrideMeasurement;

    return this;
  }

  /**
   * Returns the preferred measurement override applied to all children, or {@code null} if each child
   * reports its own preferred size.
   *
   * @return the preferred override, or {@code null}
   */
  public Double getPreferredOverrideMeasurement () {

    return preferredOverrideMeasurement;
  }

  /**
   * Sets an override that clamps every child's preferred measurement to the given value.
   *
   * @param preferredOverrideMeasurement the override value, or {@code null} to use each child's own preferred size
   * @return this box for method chaining
   */
  public ParallelBox setPreferredOverrideMeasurement (Double preferredOverrideMeasurement) {

    this.preferredOverrideMeasurement = preferredOverrideMeasurement;

    return this;
  }

  /**
   * Returns the maximum measurement override applied to all children, or {@code null} if each child
   * reports its own maximum.
   *
   * @return the maximum override, or {@code null}
   */
  public Double getMaximumOverrideMeasurement () {

    return maximumOverrideMeasurement;
  }

  /**
   * Sets an override that clamps every child's maximum measurement to the given value.
   *
   * @param maximumOverrideMeasurement the override value, or {@code null} to use each child's own maximum
   * @return this box for method chaining
   */
  public ParallelBox setMaximumOverrideMeasurement (Double maximumOverrideMeasurement) {

    this.maximumOverrideMeasurement = maximumOverrideMeasurement;

    return this;
  }

  /**
   * Returns the minimum size this box requires along the given axis, using the minimum override if set.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching; may be {@code null}
   * @return the minimum size along the axis
   */
  @Override
  public double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MINIMUM, minimumOverrideMeasurement, tailor);
  }

  /**
   * Returns the preferred size this box requests along the given axis, using the preferred override if set.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching; may be {@code null}
   * @return the preferred size along the axis
   */
  @Override
  public double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.PREFERRED, preferredOverrideMeasurement, tailor);
  }

  /**
   * Returns the maximum size this box can occupy along the given axis, using the maximum override if set.
   *
   * @param bias   the axis of measurement
   * @param tailor the layout tailor for recursive measurement caching; may be {@code null}
   * @return the maximum size along the axis
   */
  @Override
  public double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MAXIMUM, maximumOverrideMeasurement, tailor);
  }

  /**
   * Computes the aggregate measurement for this box along the given axis by determining the maximum
   * ascent and descent across all elements, applying any measurement override and accounting for
   * baseline alignment.
   *
   * @param bias                        the axis along which to measure
   * @param tapeMeasure                 the category of measurement to compute
   * @param unbiasedMeasurementOverride optional fixed value to substitute for each element's natural measurement; {@code null} uses element's own measurement
   * @param tailor                      the layout tailor for recursive caching; may be {@code null}
   * @return the combined ascent-plus-descent measurement for this box
   */
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

  /**
   * Lays out each element at the container's starting position, sizing it within the available
   * measurement and positioning it according to the configured alignment.
   *
   * @param bias                 the axis along which to position elements
   * @param containerPosition    the starting offset along the axis
   * @param containerMeasurement the total space available along the axis
   * @param tailor               the {@link LayoutTailor} coordinating the full layout pass
   */
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
              if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().bias())) {
                tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
              } else {
                switch (getLayout().getContainer().getPlatform().getOrientation().flow()) {
                  case FIRST_TO_LAST:
                    tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
                    break;
                  case LAST_TO_FIRST:
                    tailor.applyLayout(bias, containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().flow().name());
                }
              }
              break;
            case TRAILING:
              if (!bias.equals(getLayout().getContainer().getPlatform().getOrientation().bias())) {
                tailor.applyLayout(bias, containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
              } else {
                switch (getLayout().getContainer().getPlatform().getOrientation().flow()) {
                  case FIRST_TO_LAST:
                    tailor.applyLayout(bias, containerPosition + containerMeasurement - elementMeasurement, elementMeasurement, element);
                    break;
                  case LAST_TO_FIRST:
                    tailor.applyLayout(bias, containerPosition, elementMeasurement, element);
                    break;
                  default:
                    throw new UnknownSwitchCaseException(getLayout().getContainer().getPlatform().getOrientation().flow().name());
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

              if ((top = baselineCalculations.getIdealizedBaseline() - baselineCalculations.getElementAscentsDescents()[index].first()) < 0) {
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

        index++;
      }
    }
  }
}
