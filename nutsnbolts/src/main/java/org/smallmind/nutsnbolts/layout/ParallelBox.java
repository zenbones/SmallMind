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
 * Box that stacks elements in parallel along the axis opposite the provided {@link Bias}, aligning them
 * according to the specified {@link Alignment}. Growth and shrink factors still apply along the original bias.
 */
public class ParallelBox extends Box<ParallelBox> {

  private Alignment alignment;
  private Double minimumOverrideMeasurement;
  private Double preferredOverrideMeasurement;
  private Double maximumOverrideMeasurement;

  // Lays out items one after another, in the direction *opposite* to the Bias, but with growth/shrink values still in the bias direction

  /**
   * Creates a parallel box with default leading alignment.
   *
   * @param layout the owning layout
   */
  protected ParallelBox (ParaboxLayout layout) {

    this(layout, Alignment.LEADING);
  }

  /**
   * Creates a parallel box with the given alignment.
   *
   * @param layout    the owning layout
   * @param alignment alignment applied when distributing elements
   */
  protected ParallelBox (ParaboxLayout layout, Alignment alignment) {

    super(ParallelBox.class, layout);

    this.alignment = alignment;
  }

  /**
   * Returns the alignment for this box.
   *
   * @return the alignment
   */
  public Alignment getAlignment () {

    return alignment;
  }

  /**
   * Sets the alignment for this box.
   *
   * @param alignment the alignment to use
   * @return this box for chaining
   */
  public ParallelBox setAlignment (Alignment alignment) {

    this.alignment = alignment;

    return this;
  }

  /**
   * @return override for minimum measurement, if any
   */
  public Double getMinimumOverrideMeasurement () {

    return minimumOverrideMeasurement;
  }

  /**
   * Sets an override for the minimum measurement applied to all children.
   *
   * @param minimumOverrideMeasurement override value, or {@code null} to use child minimums
   * @return this box for chaining
   */
  public ParallelBox setMinimumOverrideMeasurement (Double minimumOverrideMeasurement) {

    this.minimumOverrideMeasurement = minimumOverrideMeasurement;

    return this;
  }

  /**
   * @return override for preferred measurement, if any
   */
  public Double getPreferredOverrideMeasurement () {

    return preferredOverrideMeasurement;
  }

  /**
   * Sets an override for the preferred measurement applied to all children.
   *
   * @param preferredOverrideMeasurement override value, or {@code null} to use child preferred measurements
   * @return this box for chaining
   */
  public ParallelBox setPreferredOverrideMeasurement (Double preferredOverrideMeasurement) {

    this.preferredOverrideMeasurement = preferredOverrideMeasurement;

    return this;
  }

  /**
   * @return override for maximum measurement, if any
   */
  public Double getMaximumOverrideMeasurement () {

    return maximumOverrideMeasurement;
  }

  /**
   * Sets an override for the maximum measurement applied to all children.
   *
   * @param maximumOverrideMeasurement override value, or {@code null} to use child maximums
   * @return this box for chaining
   */
  public ParallelBox setMaximumOverrideMeasurement (Double maximumOverrideMeasurement) {

    this.maximumOverrideMeasurement = maximumOverrideMeasurement;

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double calculateMinimumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MINIMUM, minimumOverrideMeasurement, tailor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double calculatePreferredMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.PREFERRED, preferredOverrideMeasurement, tailor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double calculateMaximumMeasurement (Bias bias, LayoutTailor tailor) {

    return calculateMeasurement(bias, TapeMeasure.MAXIMUM, maximumOverrideMeasurement, tailor);
  }

  /**
   * Computes the measurement along the specified axis, taking into account optional overrides and baseline alignment.
   *
   * @param bias                        the axis along which to measure
   * @param tapeMeasure                 which measurement to compute
   * @param unbiasedMeasurementOverride optional override applied uniformly
   * @param tailor                      layout tailor used for caching
   * @return the required measurement
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
   * {@inheritDoc}
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
      }
    }
  }
}
